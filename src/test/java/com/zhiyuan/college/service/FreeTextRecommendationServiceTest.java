package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.auth.AuthService;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class FreeTextRecommendationServiceTest {

    @Mock
    private AiRequirementParserService parserService;

    @Mock
    private RecommendationService recommendationService;

    @Mock
    private AiExplanationService aiExplanationService;

    @Mock
    private AiAdviceSummaryService aiAdviceSummaryService;

    @Mock
    private RecommendationHintService recommendationHintService;

    @Mock
    private AuthService authService;

    @Mock
    private RecommendationTrackingService recommendationTrackingService;

    private FreeTextRecommendationService service;

    @BeforeEach
    void setUp() {
        service = new FreeTextRecommendationService(
                parserService,
                recommendationService,
                aiExplanationService,
                aiAdviceSummaryService,
                recommendationHintService,
                authService,
                recommendationTrackingService
        );
    }

    @Test
    void execute_shouldCallRecommendationOnceForSingleMajor() {
        ParsedRequirement parsed = buildParsedRequirement(List.of("软件工程"));
        prepareExecution(parsed);

        service.execute(buildRequest(), "request-1", null);

        verify(recommendationService).recommend(argThat(request ->
                request.getRecommendationMode() == RecommendationMode.MAJOR_FIRST
                        && "软件工程".equals(request.getMajorKeyword())));
        verify(authService, never()).updateScore(any(), any());
    }

    @Test
    void execute_shouldCallEachMajorExactlyOnce() {
        ParsedRequirement parsed = buildParsedRequirement(List.of("计算机科学与技术", "软件工程"));
        prepareExecution(parsed);

        service.execute(buildRequest(), "request-2", null);

        verify(recommendationService).recommend(argThat(request ->
                "计算机科学与技术".equals(request.getMajorKeyword())));
        verify(recommendationService).recommend(argThat(request ->
                "软件工程".equals(request.getMajorKeyword())));
    }

    private void prepareExecution(ParsedRequirement parsed) {
        AiRequirementParserService.ParseTrace trace = new AiRequirementParserService.ParseTrace(
                "local-rule", null, "LOCAL_RULE_ONLY", true, null, null);
        when(parserService.parseWithTrace("浙江物理620分，想学计算机")).thenReturn(
                new AiRequirementParserService.ParseResult(parsed, trace));
        when(recommendationService.recommend(any(RecommendationRequest.class))).thenAnswer(invocation -> {
            RecommendationRequest request = invocation.getArgument(0);
            return new RecommendationResponse(
                    "recommend-" + request.getMajorKeyword(),
                    RecommendationMode.MAJOR_FIRST,
                    26000,
                    List.of(),
                    List.of(),
                    List.of(),
                    "summary",
                    List.of()
            );
        });
        when(aiExplanationService.buildSummary(any(RecommendationRequest.class), eq(0), eq(26000), eq(false)))
                .thenReturn("summary");
        when(aiAdviceSummaryService.summarize(anyString())).thenReturn("");
        when(recommendationHintService.buildTips(parsed, 0)).thenReturn(List.of());
    }

    private ParsedRequirement buildParsedRequirement(List<String> majors) {
        ParsedRequirement parsed = new ParsedRequirement();
        parsed.setScore(620);
        parsed.setCandidateProvince("浙江");
        parsed.setSubjectType(SubjectType.PHYSICS);
        parsed.setRecommendationMode(RecommendationMode.MAJOR_FIRST);
        parsed.setNormalizedMajors(majors);
        return parsed;
    }

    private FreeTextRecommendationRequest buildRequest() {
        FreeTextRecommendationRequest request = new FreeTextRecommendationRequest();
        request.setRequirementText("浙江物理620分，想学计算机");
        return request;
    }
}
