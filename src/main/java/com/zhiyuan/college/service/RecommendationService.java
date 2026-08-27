package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.service.RecommendationPolicyService.RecommendationDecision;
import com.zhiyuan.college.util.UniversityTagUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationService {

    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final ScoreRankMappingService scoreRankMappingService;
    private final RecommendationPolicyService recommendationPolicyService;
    private final AiExplanationService aiExplanationService;
    private final RecommendationHintService recommendationHintService;
    private final RecommendationCacheService recommendationCacheService;

    public RecommendationService(AdmissionCutoffMapper admissionCutoffMapper,
                                 MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                 ScoreRankMappingService scoreRankMappingService,
                                 RecommendationPolicyService recommendationPolicyService,
                                 AiExplanationService aiExplanationService,
                                 RecommendationHintService recommendationHintService,
                                 RecommendationCacheService recommendationCacheService) {
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.scoreRankMappingService = scoreRankMappingService;
        this.recommendationPolicyService = recommendationPolicyService;
        this.aiExplanationService = aiExplanationService;
        this.recommendationHintService = recommendationHintService;
        this.recommendationCacheService = recommendationCacheService;
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
        RecommendationResponse cached = recommendationCacheService.getRecommendation(request);
        if (cached != null) {
            return cached;
        }

        RecommendationResponse response = mode == RecommendationMode.MAJOR_FIRST
                ? recommendMajorFirst(request)
                : recommendSchoolFirst(request);
        recommendationCacheService.cacheRecommendation(request, response);
        return response;
    }

    private RecommendationResponse recommendSchoolFirst(RecommendationRequest request) {

        List<AdmissionCutoffWithUniversity> cutoffs = admissionCutoffMapper.findLatestByProvinceAndSubject(
                request.getProvince(), request.getSubjectType().getDbValue());
        Integer userRank = scoreRankMappingService.resolveUserRank(
                request.getProvince(), request.getSubjectType().getDbValue(), request.getScore());

        if (cutoffs.isEmpty()) {
            String provinceLabel = request.getProvince() == null || request.getProvince().isBlank()
                    ? "当前省份"
                    : request.getProvince();
            return new RecommendationResponse(
                    UUID.randomUUID().toString(),
                    RecommendationMode.SCHOOL_FIRST,
                    userRank,
                    List.of(),
                    List.of(),
                    List.of(),
                    provinceLabel + "暂无" + request.getSubjectType().getDbValue() + "类院校录取数据，暂时无法生成学校优先推荐。",
                    List.of("请切换到已有比赛验证数据的省份和科类后重试。")
            );
        }

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
                    cutoff.getUniversityId(),
                    cutoff.getUniversityName(),
                    null,
                    cutoff.getUniversityProvince(),
                    cutoff.getUniversityTier(),
                    cutoff.getIs985(),
                    cutoff.getIs211(),
                    cutoff.getIsDoubleFirstClass(),
                    UniversityTagUtils.buildSchoolTags(
                            cutoff.getIs985(),
                            cutoff.getIs211(),
                            cutoff.getIsDoubleFirstClass(),
                            cutoff.getUniversityTier()),
                    cutoff.getUniversityTags(),
                    cutoff.getCutoffScore(),
                    decision.scoreGap(),
                    decision.userRank(),
                    decision.minRank(),
                    decision.rankGap(),
                    decision.admissionProbability(),
                    decision.recommendationBasis(),
                    decision.strategy().name(),
                    null,
                    null,
                    null,
                    null
            );
            item.setDataKind(cutoff.getDataKind());
            item.setCalibrationSource(cutoff.getCalibrationSource());
            item.setSimulationRule(cutoff.getSimulationRule());
            switch (decision.strategy()) {
                case RUSH -> rush.add(item);
                case SAFE -> safe.add(item);
                case GUARANTEE -> guarantee.add(item);
            }
        }

        sortRecommendations(rush);
        sortRecommendations(safe);
        sortRecommendations(guarantee);

        aiExplanationService.enrichItems(request, rush);
        aiExplanationService.enrichItems(request, safe);
        aiExplanationService.enrichItems(request, guarantee);

        int total = rush.size() + safe.size() + guarantee.size();
        String summary = aiExplanationService.buildSummary(request, total, userRank, hasRankBasedItem(rush, safe, guarantee));
        List<String> tips = recommendationHintService.buildTips(request, total);

        return new RecommendationResponse(
                UUID.randomUUID().toString(),
                RecommendationMode.SCHOOL_FIRST,
                userRank,
                rush,
                safe,
                guarantee,
                summary,
                tips
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
                    cutoff.getUniversityId(),
                    cutoff.getUniversityName(),
                    cutoff.getMajorName(),
                    cutoff.getUniversityProvince(),
                    cutoff.getUniversityTier(),
                    cutoff.getIs985(),
                    cutoff.getIs211(),
                    cutoff.getIsDoubleFirstClass(),
                    UniversityTagUtils.buildSchoolTags(
                            cutoff.getIs985(),
                            cutoff.getIs211(),
                            cutoff.getIsDoubleFirstClass(),
                            cutoff.getUniversityTier()),
                    cutoff.getUniversityTags(),
                    cutoff.getCutoffScore(),
                    decision.scoreGap(),
                    decision.userRank(),
                    decision.minRank(),
                    decision.rankGap(),
                    decision.admissionProbability(),
                    decision.recommendationBasis(),
                    decision.strategy().name(),
                    null,
                    null,
                    null,
                    null
            );
            item.setDataKind(cutoff.getDataKind());
            item.setCalibrationSource(cutoff.getCalibrationSource());
            item.setSimulationRule(cutoff.getSimulationRule());
            switch (decision.strategy()) {
                case RUSH -> rush.add(item);
                case SAFE -> safe.add(item);
                case GUARANTEE -> guarantee.add(item);
            }
        }

        sortRecommendations(rush);
        sortRecommendations(safe);
        sortRecommendations(guarantee);

        aiExplanationService.enrichItems(request, rush);
        aiExplanationService.enrichItems(request, safe);
        aiExplanationService.enrichItems(request, guarantee);

        int total = rush.size() + safe.size() + guarantee.size();
        String summary = aiExplanationService.buildSummary(request, total, userRank, hasRankBasedItem(rush, safe, guarantee));
        List<String> tips = recommendationHintService.buildTips(request, total);

        return new RecommendationResponse(
                UUID.randomUUID().toString(),
                RecommendationMode.MAJOR_FIRST,
                userRank,
                rush,
                safe,
                guarantee,
                summary,
                tips
        );
    }

    private RecommendationMode resolveMode(RecommendationRequest request) {
        return request.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST
                : request.getRecommendationMode();
    }

    private void sortRecommendations(List<RecommendationItemResponse> items) {
        items.sort(recommendationPolicyService.recommendationComparator());
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
