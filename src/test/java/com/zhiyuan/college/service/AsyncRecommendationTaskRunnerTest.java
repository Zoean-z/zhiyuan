package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import java.time.Duration;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncRecommendationTaskRunnerTest {

    @Mock
    private RecommendationTrackingService recommendationTrackingService;

    @Mock
    private FreeTextRecommendationService freeTextRecommendationService;

    @Mock
    private HistoryService historyService;

    @Mock
    private UserAccountMapper userAccountMapper;

    private AsyncRecommendationTaskRunner taskRunner;

    @BeforeEach
    void setUp() {
        taskRunner = new AsyncRecommendationTaskRunner(
                recommendationTrackingService,
                freeTextRecommendationService,
                historyService,
                userAccountMapper,
                Duration.ofMinutes(5)
        );
    }

    @Test
    void processTextTask_shouldIgnoreDuplicateWhenTaskCannotBeClaimed() {
        RecommendationTaskMessage message = message();
        when(recommendationTrackingService.tryMarkTaskRunning(eq(11L), any(Duration.class))).thenReturn(false);

        taskRunner.processTextTask(message, 0, 3);

        verify(freeTextRecommendationService, never()).execute(any(), any(), any());
    }

    @Test
    void processTextTask_shouldResetPendingWhenBrokerCanRetry() {
        RecommendationTaskMessage message = message();
        UserAccount user = new UserAccount();
        when(recommendationTrackingService.tryMarkTaskRunning(eq(11L), any(Duration.class))).thenReturn(true);
        when(userAccountMapper.findByIdCompat(7L)).thenReturn(user);
        when(freeTextRecommendationService.execute(any(), eq("request-11"), eq(user)))
                .thenThrow(new IllegalStateException("temporary failure"));

        assertThrows(IllegalStateException.class, () -> taskRunner.processTextTask(message, 1, 3));

        verify(recommendationTrackingService).markTaskPendingForRetry(
                eq(11L),
                eq("temporary failure"),
                any(Long.class)
        );
        verify(recommendationTrackingService, never()).markTaskFailed(eq(11L), any(), any(Long.class));
    }

    @Test
    void processTextTask_shouldMarkFailedAtRetryLimit() {
        RecommendationTaskMessage message = message();
        UserAccount user = new UserAccount();
        when(recommendationTrackingService.tryMarkTaskRunning(eq(11L), any(Duration.class))).thenReturn(true);
        when(userAccountMapper.findByIdCompat(7L)).thenReturn(user);
        when(freeTextRecommendationService.execute(any(), eq("request-11"), eq(user)))
                .thenThrow(new IllegalStateException("permanent failure"));

        assertThrows(IllegalStateException.class, () -> taskRunner.processTextTask(message, 3, 3));

        verify(recommendationTrackingService).markTaskFailed(
                eq(11L),
                eq("permanent failure"),
                any(Long.class)
        );
        verify(recommendationTrackingService, never()).markTaskPendingForRetry(eq(11L), any(), any(Long.class));
    }

    private RecommendationTaskMessage message() {
        FreeTextRecommendationRequest request = new FreeTextRecommendationRequest();
        request.setRequirementText("推荐江苏的211学校");
        return new RecommendationTaskMessage(11L, 7L, "request-11", request);
    }
}
