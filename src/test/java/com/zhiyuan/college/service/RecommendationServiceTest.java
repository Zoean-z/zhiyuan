package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.config.RecommendationScoringProperties;
import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceTest {

    @Mock
    private AdmissionCutoffMapper admissionCutoffMapper;

    @Mock
    private MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;

    @Mock
    private ScoreRankMappingService scoreRankMappingService;

    @Mock
    private AiExplanationService aiExplanationService;

    @Mock
    private RecommendationHintService recommendationHintService;

    @Mock
    private RecommendationCacheService recommendationCacheService;

    private RecommendationPolicyService recommendationPolicyService;

    @InjectMocks
    private RecommendationService recommendationService;

    @BeforeEach
    void setUp() {
        recommendationPolicyService = new RecommendationPolicyService(new RecommendationScoringProperties());
        recommendationService = new RecommendationService(
                admissionCutoffMapper,
                majorAdmissionCutoffMapper,
                scoreRankMappingService,
                recommendationPolicyService,
                aiExplanationService,
                recommendationHintService,
                recommendationCacheService
        );
    }

    @Test
    void recommend_shouldReturnCachedResponseWhenPresent() {
        RecommendationRequest request = buildSchoolFirstRequest();
        RecommendationResponse cached = new RecommendationResponse(
                "cached-request",
                RecommendationMode.SCHOOL_FIRST,
                26000,
                List.of(),
                List.of(),
                List.of(),
                "cached summary",
                List.of()
        );
        when(recommendationCacheService.getRecommendation(request)).thenReturn(cached);

        RecommendationResponse response = recommendationService.recommend(request);

        assertSame(cached, response);
        verify(admissionCutoffMapper, never()).findLatestByProvinceAndSubject(any(), any());
        verify(recommendationCacheService, never()).cacheRecommendation(any(), any());
    }

    @Test
    void recommend_shouldThrowBadRequestWhenMajorFirstWithoutKeyword() {
        RecommendationRequest request = buildSchoolFirstRequest();
        request.setRecommendationMode(RecommendationMode.MAJOR_FIRST);
        request.setMajorKeyword("   ");

        ResponseStatusException exception = assertThrows(
                ResponseStatusException.class,
                () -> recommendationService.recommend(request)
        );

        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
        assertEquals("majorKeyword is required when recommendationMode is MAJOR_FIRST", exception.getReason());
    }

    @Test
    void recommend_shouldBuildGroupedSchoolRecommendationsWithoutSpringContext() {
        RecommendationRequest request = buildSchoolFirstRequest();
        when(recommendationCacheService.getRecommendation(request)).thenReturn(null);
        when(scoreRankMappingService.resolveUserRank("浙江", "物理", 620)).thenReturn(26000);
        when(admissionCutoffMapper.findLatestByProvinceAndSubject("浙江", "物理")).thenReturn(List.of(
                buildCutoff(1L, "冲刺大学", "浙江", "985", true, true, true, "综合类", 658, 25000),
                buildCutoff(2L, "稳妥大学", "浙江", "双一流", false, false, true, "综合类", 612, 28000),
                buildCutoff(3L, "保底大学", "浙江", "普通", false, false, false, "综合类", 585, 55000)
        ));
        when(recommendationHintService.buildTips(eq(request), eq(3))).thenReturn(List.of("tip-1"));
        doAnswer(invocation -> {
            @SuppressWarnings("unchecked")
            List<RecommendationItemResponse> items = invocation.getArgument(1, List.class);
            for (RecommendationItemResponse item : items) {
                item.setStrategyLabel(item.getStrategy());
                item.setRiskScore(100 - item.getAdmissionProbability());
                item.setMatchReasons(List.of(item.getUniversityName()));
                item.setExplanation("explanation-" + item.getUniversityName());
            }
            return null;
        }).when(aiExplanationService).enrichItems(eq(request), any());
        when(aiExplanationService.buildSummary(request, 3, 26000, true)).thenReturn("summary-3");

        RecommendationResponse response = recommendationService.recommend(request);

        assertNotNull(response);
        assertEquals(RecommendationMode.SCHOOL_FIRST, response.getRecommendationMode());
        assertEquals(26000, response.getUserRank());
        assertEquals(1, response.getRush().size());
        assertEquals(1, response.getSafe().size());
        assertEquals(1, response.getGuarantee().size());
        assertEquals("冲刺大学", response.getRush().get(0).getUniversityName());
        assertEquals("稳妥大学", response.getSafe().get(0).getUniversityName());
        assertEquals("保底大学", response.getGuarantee().get(0).getUniversityName());
        assertEquals("summary-3", response.getSummary());
        assertEquals(List.of("tip-1"), response.getTips());
        verify(recommendationCacheService).cacheRecommendation(eq(request), any(RecommendationResponse.class));
    }

    @Test
    void recommend_shouldExplainWhenProvinceHasNoSchoolCutoffCoverage() {
        RecommendationRequest request = buildSchoolFirstRequest();
        when(recommendationCacheService.getRecommendation(request)).thenReturn(null);
        when(admissionCutoffMapper.findLatestByProvinceAndSubject("浙江", "物理")).thenReturn(List.of());
        when(scoreRankMappingService.resolveUserRank("浙江", "物理", 620)).thenReturn(26000);

        RecommendationResponse response = recommendationService.recommend(request);

        assertEquals(0, response.getRush().size());
        assertEquals(0, response.getSafe().size());
        assertEquals(0, response.getGuarantee().size());
        assertEquals("浙江暂无物理类院校录取数据，暂时无法生成学校优先推荐。", response.getSummary());
        assertEquals(List.of("请切换到已有比赛验证数据的省份和科类后重试。"), response.getTips());
        verify(aiExplanationService, never()).buildSummary(any(), anyInt(), any(), anyBoolean());
    }

    @Test
    void recommend_shouldKeepAllQualifiedSchoolsInsteadOfFivePerGroup() {
        RecommendationRequest request = buildSchoolFirstRequest();
        when(recommendationCacheService.getRecommendation(request)).thenReturn(null);
        when(scoreRankMappingService.resolveUserRank("浙江", "物理", 620)).thenReturn(26000);
        when(admissionCutoffMapper.findLatestByProvinceAndSubject("浙江", "物理")).thenReturn(List.of(
                buildCutoff(1L, "保底大学1", "浙江", "普通", false, false, false, "综合类", 580, 50000),
                buildCutoff(2L, "保底大学2", "浙江", "普通", false, false, false, "综合类", 581, 50000),
                buildCutoff(3L, "保底大学3", "浙江", "普通", false, false, false, "综合类", 582, 50000),
                buildCutoff(4L, "保底大学4", "浙江", "普通", false, false, false, "综合类", 583, 50000),
                buildCutoff(5L, "保底大学5", "浙江", "普通", false, false, false, "综合类", 584, 50000),
                buildCutoff(6L, "保底大学6", "浙江", "普通", false, false, false, "综合类", 585, 50000)
        ));

        RecommendationResponse response = recommendationService.recommend(request);

        assertEquals(6, response.getGuarantee().size());
        verify(aiExplanationService).enrichItems(eq(request), eq(response.getGuarantee()));
    }

    private RecommendationRequest buildSchoolFirstRequest() {
        RecommendationRequest request = new RecommendationRequest();
        request.setScore(620);
        request.setProvince("浙江");
        request.setSubjectType(SubjectType.PHYSICS);
        request.setRecommendationMode(RecommendationMode.SCHOOL_FIRST);
        return request;
    }

    private AdmissionCutoffWithUniversity buildCutoff(Long universityId,
                                                      String universityName,
                                                      String province,
                                                      String tier,
                                                      boolean is985,
                                                      boolean is211,
                                                      boolean isDoubleFirstClass,
                                                      String tags,
                                                      Integer cutoffScore,
                                                      Integer minRank) {
        AdmissionCutoffWithUniversity cutoff = new AdmissionCutoffWithUniversity();
        cutoff.setUniversityId(universityId);
        cutoff.setUniversityName(universityName);
        cutoff.setUniversityProvince(province);
        cutoff.setUniversityTier(tier);
        cutoff.setIs985(is985);
        cutoff.setIs211(is211);
        cutoff.setIsDoubleFirstClass(isDoubleFirstClass);
        cutoff.setUniversityTags(tags);
        cutoff.setCutoffScore(cutoffScore);
        cutoff.setMinRank(minRank);
        return cutoff;
    }
}
