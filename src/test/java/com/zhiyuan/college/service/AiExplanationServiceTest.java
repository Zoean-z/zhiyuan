package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AiExplanationServiceTest {

    private AiExplanationService aiExplanationService;

    @BeforeEach
    void setUp() {
        aiExplanationService = new AiExplanationService();
    }

    @Test
    void enrichItems_shouldPopulateStrategyLabelRiskScoreAndMatchReasons() {
        RecommendationRequest request = buildRequest(RecommendationMode.MAJOR_FIRST, "计算机");
        RecommendationItemResponse item = new RecommendationItemResponse(
                RecommendationMode.MAJOR_FIRST,
                1L,
                "宁波大学",
                "计算机科学与技术",
                "浙江",
                "双一流",
                false,
                false,
                true,
                List.of("双一流"),
                "综合类",
                612,
                8,
                26000,
                28000,
                2000,
                57,
                "RANK",
                "SAFE",
                null,
                null,
                null,
                null
        );

        aiExplanationService.enrichItems(request, new ArrayList<>(List.of(item)));

        assertEquals("稳妥", item.getStrategyLabel());
        assertEquals(43, item.getRiskScore());
        assertFalse(item.getMatchReasons().isEmpty());
        assertTrue(item.getMatchReasons().stream().anyMatch(reason -> reason.contains("位次")));
        assertTrue(item.getMatchReasons().stream().anyMatch(reason -> reason.contains("计算机")));
        assertTrue(item.getMatchReasons().stream().anyMatch(reason -> reason.contains("双一流")));
        assertTrue(item.getExplanation().contains("规则测算录取概率"));
    }

    @Test
    void buildSummary_shouldDescribeRankBasedSchoolRecommendations() {
        RecommendationRequest request = buildRequest(RecommendationMode.SCHOOL_FIRST, null);

        String summary = aiExplanationService.buildSummary(request, 5, 26000, true);

        assertTrue(summary.contains("浙江"));
        assertTrue(summary.contains("物理"));
        assertTrue(summary.contains("620"));
        assertTrue(summary.contains("26000"));
        assertTrue(summary.contains("5"));
    }

    private RecommendationRequest buildRequest(RecommendationMode mode, String majorKeyword) {
        RecommendationRequest request = new RecommendationRequest();
        request.setProvince("浙江");
        request.setScore(620);
        request.setSubjectType(SubjectType.PHYSICS);
        request.setRecommendationMode(mode);
        request.setMajorKeyword(majorKeyword);
        return request;
    }
}
