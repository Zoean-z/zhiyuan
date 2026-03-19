package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class RecommendationPolicyService {

    private static final String BASIS_RANK = "RANK";
    private static final String BASIS_SCORE = "SCORE";

    public RecommendationDecision evaluate(Integer userScore,
                                           Integer userRank,
                                           AdmissionCutoffWithUniversity cutoff) {
        if (userRank != null && cutoff.getMinRank() != null) {
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

        if (userScore != null && cutoff.getCutoffScore() != null) {
            int scoreGap = userScore - cutoff.getCutoffScore();
            StrategyType strategy = classifyByScore(scoreGap);
            if (strategy == null) {
                return null;
            }
            return new RecommendationDecision(
                    BASIS_SCORE,
                    strategy,
                    scoreGap,
                    cutoff.getMinRank(),
                    userRank,
                    cutoff.getMinRank() == null || userRank == null ? null : cutoff.getMinRank() - userRank,
                    estimateProbabilityByScore(scoreGap)
            );
        }
        return null;
    }

    public Comparator<RecommendationItemResponse> recommendationComparator() {
        return Comparator
                .comparing(RecommendationItemResponse::getAdmissionProbability, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItemResponse::getRankGap, Comparator.nullsLast(Comparator.reverseOrder()))
                .thenComparing(RecommendationItemResponse::getScoreGap, Comparator.nullsLast(Comparator.reverseOrder()))
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

    private StrategyType classifyByScore(int scoreGap) {
        if (scoreGap >= -10 && scoreGap <= 5) {
            return StrategyType.RUSH;
        }
        if (scoreGap >= 6 && scoreGap <= 20) {
            return StrategyType.SAFE;
        }
        if (scoreGap >= 21) {
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

    private int estimateProbabilityByScore(int scoreGap) {
        int base = 58 + scoreGap;
        if (scoreGap < 0) {
            base = 45 + scoreGap * 2;
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
