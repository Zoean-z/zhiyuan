package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskSubmitResponse;
import com.zhiyuan.college.model.entity.RecommendationTask;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import com.zhiyuan.college.service.messaging.RecommendationTaskPublisher;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AsyncRecommendationTaskServiceTest {

    @Mock
    private RecommendationTrackingService recommendationTrackingService;

    @Mock
    private RecommendationTaskPublisher recommendationTaskPublisher;

    @InjectMocks
    private AsyncRecommendationTaskService asyncRecommendationTaskService;

    @Test
    void submitTextTask_shouldCreatePendingTaskAndPublishMessage() {
        FreeTextRecommendationRequest request = request();
        RecommendationTask task = task();
        when(recommendationTrackingService.createPendingTextTask(eq(7L), any(), eq(request))).thenReturn(task);

        FreeTextRecommendationTaskSubmitResponse response = asyncRecommendationTaskService.submitTextTask(7L, request);

        assertEquals(11L, response.getTaskId());
        assertEquals("PENDING", response.getStatus());
        ArgumentCaptor<RecommendationTaskMessage> messageCaptor = ArgumentCaptor.forClass(RecommendationTaskMessage.class);
        verify(recommendationTaskPublisher).publish(messageCaptor.capture());
        assertEquals(11L, messageCaptor.getValue().taskId());
        assertEquals(7L, messageCaptor.getValue().userId());
        assertEquals(request, messageCaptor.getValue().request());
    }

    @Test
    void submitTextTask_shouldMarkFailedWhenPublishFails() {
        FreeTextRecommendationRequest request = request();
        RecommendationTask task = task();
        when(recommendationTrackingService.createPendingTextTask(eq(7L), any(), eq(request))).thenReturn(task);
        doThrow(new IllegalStateException("broker unavailable"))
                .when(recommendationTaskPublisher).publish(any(RecommendationTaskMessage.class));

        FreeTextRecommendationTaskSubmitResponse response = asyncRecommendationTaskService.submitTextTask(7L, request);

        assertEquals("FAILED", response.getStatus());
        verify(recommendationTrackingService).markTaskFailed(
                eq(11L),
                contains("broker unavailable"),
                eq(0L)
        );
    }

    private FreeTextRecommendationRequest request() {
        FreeTextRecommendationRequest request = new FreeTextRecommendationRequest();
        request.setRequirementText("推荐江苏的211学校");
        return request;
    }

    private RecommendationTask task() {
        RecommendationTask task = new RecommendationTask();
        task.setId(11L);
        task.setRequestId("request-11");
        task.setStatus("PENDING");
        return task;
    }
}
