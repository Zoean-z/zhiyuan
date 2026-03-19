package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
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
    private final ScoreRankMappingService scoreRankMappingService;
    private final RecommendationPolicyService recommendationPolicyService;
    private final AiExplanationService aiExplanationService;

    public RecommendationService(AdmissionCutoffMapper admissionCutoffMapper,
                                 ScoreRankMappingService scoreRankMappingService,
                                 RecommendationPolicyService recommendationPolicyService,
                                 AiExplanationService aiExplanationService) {
        this.admissionCutoffMapper = admissionCutoffMapper;
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
                    cutoff.getUniversityName(),
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
                userRank,
                rush,
                safe,
                guarantee,
                summary
        );
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
