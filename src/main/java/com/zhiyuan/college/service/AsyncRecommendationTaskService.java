package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskSubmitResponse;
import com.zhiyuan.college.model.entity.RecommendationTask;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import com.zhiyuan.college.service.messaging.RecommendationTaskPublisher;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AsyncRecommendationTaskService {

    private final RecommendationTrackingService recommendationTrackingService;
    private final RecommendationTaskPublisher recommendationTaskPublisher;

    public AsyncRecommendationTaskService(RecommendationTrackingService recommendationTrackingService,
                                          RecommendationTaskPublisher recommendationTaskPublisher) {
        this.recommendationTrackingService = recommendationTrackingService;
        this.recommendationTaskPublisher = recommendationTaskPublisher;
    }

    public FreeTextRecommendationTaskSubmitResponse submitTextTask(Long userId,
                                                                   FreeTextRecommendationRequest request) {
        String requestId = UUID.randomUUID().toString();
        RecommendationTask task = recommendationTrackingService.createPendingTextTask(userId, requestId, request);
        try {
            recommendationTaskPublisher.publish(new RecommendationTaskMessage(task.getId(), userId, requestId, request));
            return new FreeTextRecommendationTaskSubmitResponse(task.getId(), requestId, task.getStatus());
        } catch (RuntimeException ex) {
            recommendationTrackingService.markTaskFailed(
                    task.getId(),
                    "Recommendation task publish failed: " + safeMessage(ex),
                    0L
            );
            return new FreeTextRecommendationTaskSubmitResponse(task.getId(), requestId, "FAILED");
        }
    }

    public FreeTextRecommendationTaskResponse getTextTask(Long userId, Long taskId) {
        return recommendationTrackingService.getTextTask(userId, taskId);
    }

    private String safeMessage(RuntimeException ex) {
        return ex.getMessage() == null || ex.getMessage().isBlank()
                ? ex.getClass().getSimpleName()
                : ex.getMessage();
    }
}
