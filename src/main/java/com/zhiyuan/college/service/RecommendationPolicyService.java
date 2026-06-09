package com.zhiyuan.college.service;

import com.zhiyuan.college.config.RecommendationScoringProperties;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.Comparator;
import org.springframework.stereotype.Service;

@Service
public class RecommendationPolicyService {

    private static final String BASIS_RANK = "RANK";
    private static final String BASIS_SCORE = "SCORE";
    private static final int MIN_RANK_GAP = -3000;
    private static final int RUSH_MAX_RANK_GAP = 1000;
    private static final int SAFE_MAX_RANK_GAP = 10000;
    private static final int MIN_SCORE_GAP = -10;
    private static final int RUSH_MAX_SCORE_GAP = 5;
    private static final int SAFE_MAX_SCORE_GAP = 20;

    private final RecommendationScoringProperties scoringProperties;

    public RecommendationPolicyService(RecommendationScoringProperties scoringProperties) {
        this.scoringProperties = scoringProperties;
    }

    public RecommendationDecision evaluate(Integer userScore,
                                           Integer userRank,
                                           AdmissionCutoffWithUniversity cutoff) {
        Integer scoreGap = userScore == null || cutoff.getCutoffScore() == null
                ? null : userScore - cutoff.getCutoffScore();
        Integer rankGap = userRank == null || cutoff.getMinRank() == null
                ? null : cutoff.getMinRank() - userRank;
        Integer rankProbability = rankGap == null ? null : computeRankProbability(rankGap);
        Integer scoreProbability = scoreGap == null ? null : computeScoreProbability(scoreGap);

        Integer admissionProbability = blendProbability(rankProbability, scoreProbability);
        if (admissionProbability == null || admissionProbability < scoringProperties.getMinimumProbability()) {
            return null;
        }

        StrategyType strategy = classifyByProbability(admissionProbability);
        if (strategy == null) {
            return null;
        }

        if (userRank != null && cutoff.getMinRank() != null) {
            return new RecommendationDecision(
                    BASIS_RANK,
                    strategy,
                    scoreGap,
                    cutoff.getMinRank(),
                    userRank,
                    rankGap,
                    admissionProbability
            );
        }

        if (userScore != null && cutoff.getCutoffScore() != null) {
            return new RecommendationDecision(
                    BASIS_SCORE,
                    strategy,
                    scoreGap,
                    cutoff.getMinRank(),
                    userRank,
                    cutoff.getMinRank() == null || userRank == null ? null : cutoff.getMinRank() - userRank,
                    admissionProbability
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

    private StrategyType classifyByProbability(int admissionProbability) {
        if (isWithin(admissionProbability, scoringProperties.getRush())) {
            return StrategyType.RUSH;
        }
        if (isWithin(admissionProbability, scoringProperties.getSafe())) {
            return StrategyType.SAFE;
        }
        if (isWithin(admissionProbability, scoringProperties.getGuarantee())) {
            return StrategyType.GUARANTEE;
        }
        return null;
    }

    private boolean isWithin(int value, RecommendationScoringProperties.StrategyBand band) {
        return value >= band.getMin() && value <= band.getMax();
    }

    private Integer blendProbability(Integer rankProbability, Integer scoreProbability) {
        if (rankProbability == null && scoreProbability == null) {
            return null;
        }
        if (rankProbability != null && scoreProbability != null) {
            double combined = rankProbability * scoringProperties.getRankWeight()
                    + scoreProbability * scoringProperties.getScoreWeight();
            return clamp((int) Math.round(combined), 0, 100);
        }
        return rankProbability != null ? rankProbability : scoreProbability;
    }

    private Integer computeRankProbability(int rankGap) {
        if (rankGap < MIN_RANK_GAP) {
            return null;
        }
        if (rankGap <= RUSH_MAX_RANK_GAP) {
            return scale(rankGap, MIN_RANK_GAP, RUSH_MAX_RANK_GAP,
                    scoringProperties.getRush().getMin(),
                    scoringProperties.getRush().getMax());
        }
        if (rankGap <= SAFE_MAX_RANK_GAP) {
            return scale(rankGap, RUSH_MAX_RANK_GAP + 1, SAFE_MAX_RANK_GAP,
                    scoringProperties.getSafe().getMin(),
                    scoringProperties.getSafe().getMax());
        }
        int boosted = scoringProperties.getGuarantee().getMin() + (rankGap - SAFE_MAX_RANK_GAP) / 1500;
        return clamp(boosted, scoringProperties.getGuarantee().getMin(), 96);
    }

    private Integer computeScoreProbability(int scoreGap) {
        if (scoreGap < MIN_SCORE_GAP) {
            return null;
        }
        if (scoreGap <= RUSH_MAX_SCORE_GAP) {
            return scale(scoreGap, MIN_SCORE_GAP, RUSH_MAX_SCORE_GAP,
                    scoringProperties.getRush().getMin(),
                    scoringProperties.getRush().getMax());
        }
        if (scoreGap <= SAFE_MAX_SCORE_GAP) {
            return scale(scoreGap, RUSH_MAX_SCORE_GAP + 1, SAFE_MAX_SCORE_GAP,
                    scoringProperties.getSafe().getMin(),
                    scoringProperties.getSafe().getMax());
        }
        int boosted = scoringProperties.getGuarantee().getMin() + (scoreGap - SAFE_MAX_SCORE_GAP);
        return clamp(boosted, scoringProperties.getGuarantee().getMin(), 96);
    }

    private int scale(int value, int sourceMin, int sourceMax, int targetMin, int targetMax) {
        if (sourceMin >= sourceMax) {
            return clamp(targetMin, 0, 100);
        }
        if (value <= sourceMin) {
            return clamp(targetMin, 0, 100);
        }
        if (value >= sourceMax) {
            return clamp(targetMax, 0, 100);
        }
        double ratio = (double) (value - sourceMin) / (double) (sourceMax - sourceMin);
        int scaled = (int) Math.round(targetMin + ratio * (targetMax - targetMin));
        return clamp(scaled, 0, 100);
    }

    private int clamp(int value, int min, int max) {
        return Math.max(min, Math.min(max, value));
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
