package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class RecommendationPolicyService {

    private static final String BASIS_RANK = "RANK";

    public RecommendationDecision evaluate(Integer userScore,
                                           Integer userRank,
                                           AdmissionCutoffWithUniversity cutoff) {
        if (userRank == null || cutoff.getMinRank() == null) {
            return null;
        }

        int rankGap = cutoff.getMinRank() - userRank;
        StrategyType strategy = classifyByRank(rankGap);
        if (strategy == null) {
            return null;
        }
        Integer scoreGap = userScore == null || cutoff.getCutoffScore() == null
                ? null : userScore - cutoff.getCutoffScore();
        return new RecommendationDecision(
                BASIS_RANK,
                strategy,
                scoreGap,
                cutoff.getMinRank(),
                userRank,
                rankGap,
                estimateProbabilityByRank(rankGap)
        );
    }

    public Comparator<RecommendationItemResponse> recommendationComparator() {
        return Comparator
                .comparing(RecommendationItemResponse::getAdmissionProbability, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItemResponse::getRankGap, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItemResponse::getUniversityName, Comparator.nullsLast(String::compareTo));
    }

    private StrategyType classifyByRank(int rankGap) {
        if (rankGap >= -3000 && rankGap <= 1000) {
            return StrategyType.RUSH;
        }
        if (rankGap >= 1001 && rankGap <= 10000) {
            return StrategyType.SAFE;
        }
        if (rankGap >= 10001) {
            return StrategyType.GUARANTEE;
        }
        return null;
    }

    private int estimateProbabilityByRank(int rankGap) {
        int base = 55 + rankGap / 400;
        if (rankGap < 0) {
            base = 45 + rankGap / 500;
        }
        if (base < 5) {
            return 5;
        }
        return Math.min(base, 99);
    }

    public record RecommendationDecision(String recommendationBasis,
                                         StrategyType strategy,
                                         Integer scoreGap,
                                         Integer minRank,
                                         Integer userRank,
                                         Integer rankGap,
                                         Integer admissionProbability) {
    }
}
