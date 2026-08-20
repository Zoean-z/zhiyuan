package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

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
}
