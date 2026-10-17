package com.zhiyuan.college.service;

import com.zhiyuan.college.config.RecommendationScoringProperties;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.Comparator;
import org.springframework.stereotype.Service;

/**
 * 录取概率 / 冲稳保策略的唯一口径。
 *
 * <p>{@link #explain} 返回完整拆解且<b>不做</b>最低概率过滤，供“查大学 / 院校详情 / 概率接口”使用；
     * {@link #evaluate} 在它之上叠推荐门槛。</p>
 */
@Service
public class RecommendationPolicyService {

    public static final String BASIS_RANK = "RANK";
    public static final String BASIS_SCORE = "SCORE";

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

    /**
     * 概率拆解：不过滤、不丢数据，把模型中间量全部输出。
     *
     * <p>落后位次按相对该校参考位次的比例分档，不再用固定3000名否决所有院校。
     * 位次结论作为落后考生的评分上限，不能被偏低分数线救回；两项均落后时保留分数劣势。
     * 这些分值是启发式规则评分，未经统计概率校准。</p>
     */
    public ProbabilityBreakdown explain(Integer userScore,
                                       Integer userRank,
                                       Integer cutoffScore,
                                       Integer minRank) {
        Integer scoreGap = userScore == null || cutoffScore == null ? null : userScore - cutoffScore;
        Integer rankGap = userRank == null || minRank == null || userRank <= 0 || minRank <= 0
                ? null : minRank - userRank;
        Integer rankProbability = rankGap == null ? null : computeRankProbability(rankGap, minRank);
        Integer scoreProbability = scoreGap == null ? null : computeScoreProbability(scoreGap);
        Integer probability;
        if (rankGap != null && rankGap < 0) {
            probability = Math.min(rankProbability, blendProbability(rankProbability, scoreProbability));
            if (scoreGap != null && scoreGap < MIN_SCORE_GAP) {
                probability = Math.min(probability, scoreProbability);
            }
        } else if (rankGap != null && scoreGap != null && scoreGap < MIN_SCORE_GAP) {
            // 位次已领先时，跨年份分数波动不单独推翻位次，保持原先位次优先的行为。
            probability = rankProbability;
        } else {
            probability = blendProbability(rankProbability, scoreProbability);
        }
        StrategyType strategy = probability == null ? null : classifyByProbability(probability);
        boolean recommended = probability != null
                && strategy != null
                && probability >= scoringProperties.getMinimumProbability();
        String basis = rankGap != null
                ? BASIS_RANK
                : (userScore != null && cutoffScore != null ? BASIS_SCORE : null);
        return new ProbabilityBreakdown(
                basis,
                strategy,
                scoreGap,
                rankGap,
                rankProbability,
                scoreProbability,
                probability,
                recommended
        );
    }

    public RecommendationDecision evaluate(Integer userScore,
                                           Integer userRank,
                                           AdmissionCutoffWithUniversity cutoff) {
        ProbabilityBreakdown breakdown = explain(userScore, userRank, cutoff.getCutoffScore(), cutoff.getMinRank());
        if (!breakdown.recommended() || breakdown.basis() == null) {
            return null;
        }
        return new RecommendationDecision(
                breakdown.basis(),
                breakdown.strategy(),
                breakdown.scoreGap(),
                cutoff.getMinRank(),
                userRank,
                breakdown.rankGap(),
                breakdown.probability()
        );
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

    private Integer computeRankProbability(int rankGap, int minRank) {
        if (rankGap < 0) {
            double deficit = -(double) rankGap / minRank;
            if (deficit >= 1) return 0;
            if (deficit >= 0.5) return clamp((int) Math.round(19 * (1 - deficit) / 0.5), 1, 19);
            if (deficit >= 0.2) return clamp((int) Math.round(34 - (deficit - 0.2) / 0.3 * 14), 20, 34);
            int atCutoff = scale(0, MIN_RANK_GAP, RUSH_MAX_RANK_GAP,
                    scoringProperties.getRush().getMin(), scoringProperties.getRush().getMax());
            return scale((int) Math.round((1 - deficit / 0.2) * 10000), 0, 10000,
                    scoringProperties.getRush().getMin(), atCutoff);
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
            if (scoreGap <= -30) return 0;
            if (scoreGap < -20) return scale(scoreGap, -30, -20, 1, 19);
            return scale(scoreGap, -20, MIN_SCORE_GAP, 20, 34);
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

    /** 概率模型的完整拆解（无过滤）。 */
    public record ProbabilityBreakdown(String basis,
                                       StrategyType strategy,
                                       Integer scoreGap,
                                       Integer rankGap,
                                       Integer rankProbability,
                                       Integer scoreProbability,
                                       Integer probability,
                                       boolean recommended) {
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
