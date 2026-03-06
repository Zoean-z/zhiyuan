package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationService {

    private static final int MAX_PER_GROUP = 5;

    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final AiExplanationService aiExplanationService;

    public RecommendationService(AdmissionCutoffMapper admissionCutoffMapper,
                                 AiExplanationService aiExplanationService) {
        this.admissionCutoffMapper = admissionCutoffMapper;
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

        List<RecommendationItemResponse> rush = new ArrayList<>();
        List<RecommendationItemResponse> safe = new ArrayList<>();
        List<RecommendationItemResponse> guarantee = new ArrayList<>();

        for (AdmissionCutoffWithUniversity cutoff : cutoffs) {
            int gap = request.getScore() - cutoff.getCutoffScore();
            StrategyType strategy = classify(gap);
            if (strategy == null) {
                continue;
            }
            int probability = estimateProbability(gap);
            RecommendationItemResponse item = new RecommendationItemResponse(
                    cutoff.getUniversityName(),
                    cutoff.getCutoffScore(),
                    gap,
                    probability,
                    strategy.name(),
                    null
            );
            switch (strategy) {
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
        String summary = aiExplanationService.buildSummary(request, total);

        return new RecommendationResponse(
                UUID.randomUUID().toString(),
                rush,
                safe,
                guarantee,
                summary
        );
    }

    private void sortAndLimit(List<RecommendationItemResponse> items) {
        items.sort(Comparator
                .comparing(RecommendationItemResponse::getAdmissionProbability).reversed()
                .thenComparing(RecommendationItemResponse::getScoreGap).reversed());
        if (items.size() > MAX_PER_GROUP) {
            items.subList(MAX_PER_GROUP, items.size()).clear();
        }
    }

    private StrategyType classify(int gap) {
        if (gap >= -10 && gap <= 5) {
            return StrategyType.RUSH;
        }
        if (gap >= 6 && gap <= 20) {
            return StrategyType.SAFE;
        }
        if (gap >= 21) {
            return StrategyType.GUARANTEE;
        }
        return null;
    }

    private int estimateProbability(int gap) {
        int base = 50 + gap * 2;
        if (gap < 0) {
            base = 40 + gap;
        }
        if (base < 5) {
            return 5;
        }
        return Math.min(base, 99);
    }
}
