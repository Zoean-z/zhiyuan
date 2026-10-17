package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhiyuan.college.config.RecommendationScoringProperties;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.enums.StrategyType;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class RecommendationPolicyServiceTest {

    private RecommendationPolicyService recommendationPolicyService;

    @BeforeEach
    void setUp() {
        RecommendationScoringProperties properties = new RecommendationScoringProperties();
        recommendationPolicyService = new RecommendationPolicyService(properties);
    }

    @Test
    void evaluate_shouldMapModerateRankAdvantageToSafe() {
        AdmissionCutoffWithUniversity cutoff = buildCutoff(612, 28000);

        RecommendationPolicyService.RecommendationDecision decision =
                recommendationPolicyService.evaluate(620, 26000, cutoff);

        assertNotNull(decision);
        assertEquals("RANK", decision.recommendationBasis());
        assertEquals(StrategyType.SAFE, decision.strategy());
        assertEquals(57, decision.admissionProbability());
        assertEquals(2000, decision.rankGap());
        assertEquals(8, decision.scoreGap());
    }

    @Test
    void evaluate_shouldMapStrongScoreAdvantageToGuarantee() {
        AdmissionCutoffWithUniversity cutoff = buildCutoff(590, null);

        RecommendationPolicyService.RecommendationDecision decision =
                recommendationPolicyService.evaluate(620, null, cutoff);

        assertNotNull(decision);
        assertEquals("SCORE", decision.recommendationBasis());
        assertEquals(StrategyType.GUARANTEE, decision.strategy());
        assertEquals(85, decision.admissionProbability());
        assertEquals(30, decision.scoreGap());
    }

    @Test
    void evaluate_shouldFilterOutClearlyUnsafeCandidates() {
        AdmissionCutoffWithUniversity cutoff = buildCutoff(650, 10000);

        RecommendationPolicyService.RecommendationDecision decision =
                recommendationPolicyService.evaluate(620, 20000, cutoff);

        assertNull(decision);
    }

    private AdmissionCutoffWithUniversity buildCutoff(Integer cutoffScore, Integer minRank) {
        AdmissionCutoffWithUniversity cutoff = new AdmissionCutoffWithUniversity();
        cutoff.setCutoffScore(cutoffScore);
        cutoff.setMinRank(minRank);
        cutoff.setUniversityName("Test University");
        return cutoff;
    }

    // --- L-20260921 消融实验 A0→A1：位次一票否决 ---

    @Test
    void rankImpossible_vetoesLuckyScoreCutoff() {
        // 位次差 -16314（模型区间外），但某年分数线碰巧偏低使 score 概率进入 GUARANTEE：
        // 修复前 score 单 basis 会救回该学校（消融 A0 中 264 条此类污染），修复后位次否决。
        AdmissionCutoffWithUniversity cutoff = buildCutoff(560, 8890);

        RecommendationPolicyService.RecommendationDecision decision =
                recommendationPolicyService.evaluate(588, 25204, cutoff);

        assertNull(decision);
    }

    @Test
    void rankImpossible_explainsAsZeroProbability() {
        RecommendationPolicyService.ProbabilityBreakdown breakdown =
                recommendationPolicyService.explain(588, 25204, 560, 8890);

        assertEquals(0, breakdown.probability());
        assertNull(breakdown.strategy());
        assertFalse(breakdown.recommended());
    }

    @Test
    void missingRankData_stillFallsBackToScoreBasis() {
        // 位次数据缺失（rankGap==null）不触发否决：单分数口径保留
        RecommendationPolicyService.ProbabilityBreakdown breakdown =
                recommendationPolicyService.explain(620, null, 590, null);

        assertNotNull(breakdown.probability());
        assertTrue(breakdown.probability() >= 75);
        assertTrue(breakdown.recommended());
    }

    @Test
    void eliteCutoff_relativeRankDeficitCannotBeRescuedByScore() {
        for (int score : new int[]{650, 700}) {
            var result = recommendationPolicyService.explain(score, 3131, 686, 189);
            assertEquals(0, result.probability());
            assertNull(result.strategy());
            assertFalse(result.recommended());
        }
        assertNull(recommendationPolicyService.evaluate(650, 3131, buildCutoff(686, 189)));
    }

    @Test
    void relativeRankBoundariesProduceLowScoresWithoutRecommending() {
        int[] ranks = {11999, 12000, 14999, 15000, 19999, 20000};
        int[] expected = {35, 34, 20, 19, 1, 0};
        for (int i = 0; i < ranks.length; i++) {
            var result = recommendationPolicyService.explain(null, ranks[i], null, 10000);
            assertEquals(expected[i], result.probability());
            assertEquals(i == 0, result.recommended());
        }
    }

    @Test
    void sameAbsoluteDeficitIsNotEquivalentAcrossRankRanges() {
        assertEquals(0, recommendationPolicyService.explain(null, 3131, null, 189).probability());
        var ordinary = recommendationPolicyService.explain(595, 62942, 600, 60000);
        assertTrue(ordinary.probability() >= 35);
        assertTrue(ordinary.recommended());
        assertTrue(recommendationPolicyService.explain(null, 64000, null, 60000).probability() > 0);
    }

    @Test
    void scoreDeficitsRemainKnownAndCanLimitBothNegativeIndicators() {
        int[] scores = {650, 657, 662, 667, 675, 676};
        int previous = -1;
        for (int score : scores) {
            var result = recommendationPolicyService.explain(score, null, 686, null);
            assertNotNull(result.probability());
            assertTrue(result.probability() >= previous);
            previous = result.probability();
        }
        assertEquals(0, recommendationPolicyService.explain(650, null, 686, null).probability());
        assertEquals(0, recommendationPolicyService.explain(620, 26000, 658, 25000).probability());
        assertFalse(recommendationPolicyService.explain(620, 26000, 658, 25000).recommended());
        // Leading ranks remain authoritative when cross-year scores conflict.
        assertEquals(57, recommendationPolicyService.explain(620, 26000, 660, 28000).probability());
    }

    @Test
    void rankScoreIsMonotoneAndUnknownInputsStayUnknown() {
        int previous = 101;
        for (int rank = 1; rank <= 30000; rank += 37) {
            int value = recommendationPolicyService.explain(null, rank, null, 10000).probability();
            assertTrue(value <= previous, "Worse ranks must not improve assessment");
            previous = value;
        }
        assertNull(recommendationPolicyService.explain(null, null, null, null).probability());
        assertNull(recommendationPolicyService.explain(null, 100, null, 0).probability());
    }
}
