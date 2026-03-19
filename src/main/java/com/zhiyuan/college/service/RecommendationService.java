package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.service.RecommendationPolicyService.RecommendationDecision;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationService {

    private static final int MAX_PER_GROUP = 5;

    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final ScoreRankMappingService scoreRankMappingService;
    private final RecommendationPolicyService recommendationPolicyService;
    private final AiExplanationService aiExplanationService;

    public RecommendationService(AdmissionCutoffMapper admissionCutoffMapper,
                                 MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                 ScoreRankMappingService scoreRankMappingService,
                                 RecommendationPolicyService recommendationPolicyService,
                                 AiExplanationService aiExplanationService) {
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.scoreRankMappingService = scoreRankMappingService;
        this.recommendationPolicyService = recommendationPolicyService;
        this.aiExplanationService = aiExplanationService;
    }

    public RecommendationResponse recommend(RecommendationRequest request) {
        if (request.getSubjectType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectType is required");
        }
        if (request.getScore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score is required");
        }
        RecommendationMode mode = resolveMode(request);
        request.setRecommendationMode(mode);

        if (mode == RecommendationMode.MAJOR_FIRST) {
            return recommendMajorFirst(request);
        }

        return recommendSchoolFirst(request);
    }

    private RecommendationResponse recommendSchoolFirst(RecommendationRequest request) {

        List<AdmissionCutoffWithUniversity> cutoffs = admissionCutoffMapper.findLatestByProvinceAndSubject(
                request.getProvince(), request.getSubjectType().getDbValue());
        Integer userRank = scoreRankMappingService.resolveUserRank(
                request.getProvince(), request.getSubjectType().getDbValue(), request.getScore());

        List<RecommendationItemResponse> rush = new ArrayList<>();
        List<RecommendationItemResponse> safe = new ArrayList<>();
        List<RecommendationItemResponse> guarantee = new ArrayList<>();

        for (AdmissionCutoffWithUniversity cutoff : cutoffs) {
            RecommendationDecision decision = recommendationPolicyService.evaluate(request.getScore(), userRank, cutoff);
            if (decision == null) {
                continue;
            }
            RecommendationItemResponse item = new RecommendationItemResponse(
                    RecommendationMode.SCHOOL_FIRST,
                    cutoff.getUniversityName(),
                    null,
                    cutoff.getCutoffScore(),
                    decision.scoreGap(),
                    decision.userRank(),
                    decision.minRank(),
                    decision.rankGap(),
                    decision.admissionProbability(),
                    decision.recommendationBasis(),
                    decision.strategy().name(),
                    null
            );
            switch (decision.strategy()) {
                case RUSH -> rush.add(item);
                case SAFE -> safe.add(item);
                case GUARANTEE -> guarantee.add(item);
            }
        }

        sortAndLimit(rush);
        sortAndLimit(safe);
        sortAndLimit(guarantee);

        aiExplanationService.enrichItems(request, rush);
        aiExplanationService.enrichItems(request, safe);
        aiExplanationService.enrichItems(request, guarantee);

        int total = rush.size() + safe.size() + guarantee.size();
        String summary = aiExplanationService.buildSummary(request, total, userRank, hasRankBasedItem(rush, safe, guarantee));

        return new RecommendationResponse(
                UUID.randomUUID().toString(),
                RecommendationMode.SCHOOL_FIRST,
                userRank,
                rush,
                safe,
                guarantee,
                summary
        );
    }

    private RecommendationResponse recommendMajorFirst(RecommendationRequest request) {
        String majorKeyword = request.getMajorKeyword() == null ? "" : request.getMajorKeyword().trim();
        if (majorKeyword.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "majorKeyword is required when recommendationMode is MAJOR_FIRST");
        }
        request.setMajorKeyword(majorKeyword);

        List<AdmissionCutoffWithUniversity> cutoffs = majorAdmissionCutoffMapper.findLatestByProvinceSubjectAndMajorKeyword(
                request.getProvince(), request.getSubjectType().getDbValue(), majorKeyword);
        Integer userRank = scoreRankMappingService.resolveUserRank(
                request.getProvince(), request.getSubjectType().getDbValue(), request.getScore());

        List<RecommendationItemResponse> rush = new ArrayList<>();
        List<RecommendationItemResponse> safe = new ArrayList<>();
        List<RecommendationItemResponse> guarantee = new ArrayList<>();

        for (AdmissionCutoffWithUniversity cutoff : cutoffs) {
            RecommendationDecision decision = recommendationPolicyService.evaluate(request.getScore(), userRank, cutoff);
            if (decision == null) {
                continue;
            }
            RecommendationItemResponse item = new RecommendationItemResponse(
                    RecommendationMode.MAJOR_FIRST,
                    cutoff.getUniversityName(),
                    cutoff.getMajorName(),
                    cutoff.getCutoffScore(),
                    decision.scoreGap(),
                    decision.userRank(),
                    decision.minRank(),
                    decision.rankGap(),
                    decision.admissionProbability(),
                    decision.recommendationBasis(),
                    decision.strategy().name(),
                    null
            );
            switch (decision.strategy()) {
                case RUSH -> rush.add(item);
                case SAFE -> safe.add(item);
                case GUARANTEE -> guarantee.add(item);
            }
        }

        sortAndLimit(rush);
        sortAndLimit(safe);
        sortAndLimit(guarantee);

        aiExplanationService.enrichItems(request, rush);
        aiExplanationService.enrichItems(request, safe);
        aiExplanationService.enrichItems(request, guarantee);

        int total = rush.size() + safe.size() + guarantee.size();
        String summary = aiExplanationService.buildSummary(request, total, userRank, hasRankBasedItem(rush, safe, guarantee));

        return new RecommendationResponse(
                UUID.randomUUID().toString(),
                RecommendationMode.MAJOR_FIRST,
                userRank,
                rush,
                safe,
                guarantee,
                summary
        );
    }

    private RecommendationMode resolveMode(RecommendationRequest request) {
        return request.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST
                : request.getRecommendationMode();
    }

    private void sortAndLimit(List<RecommendationItemResponse> items) {
        items.sort(recommendationPolicyService.recommendationComparator());
        if (items.size() > MAX_PER_GROUP) {
            items.subList(MAX_PER_GROUP, items.size()).clear();
        }
    }

    @SafeVarargs
    private final boolean hasRankBasedItem(List<RecommendationItemResponse>... groups) {
        for (List<RecommendationItemResponse> group : groups) {
            for (RecommendationItemResponse item : group) {
                if ("RANK".equals(item.getRecommendationBasis())) {
                    return true;
                }
            }
        }
        return false;
    }
}
