package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskSubmitResponse;
import com.zhiyuan.college.model.entity.RecommendationTask;
import org.springframework.stereotype.Service;
import java.util.UUID;

@Service
public class AsyncRecommendationTaskService {

    private final RecommendationTrackingService recommendationTrackingService;
    private final AsyncRecommendationTaskRunner asyncRecommendationTaskRunner;

    public AsyncRecommendationTaskService(RecommendationTrackingService recommendationTrackingService,
                                          AsyncRecommendationTaskRunner asyncRecommendationTaskRunner) {
        this.recommendationTrackingService = recommendationTrackingService;
        this.asyncRecommendationTaskRunner = asyncRecommendationTaskRunner;
    }

    public FreeTextRecommendationTaskSubmitResponse submitTextTask(Long userId,
                                                                   FreeTextRecommendationRequest request) {
        String requestId = UUID.randomUUID().toString();
        RecommendationTask task = recommendationTrackingService.createPendingTextTask(userId, requestId, request);
        asyncRecommendationTaskRunner.processTextTask(task.getId(), userId, request, requestId);
        return new FreeTextRecommendationTaskSubmitResponse(task.getId(), requestId, task.getStatus());
    }

    public FreeTextRecommendationTaskResponse getTextTask(Long userId, Long taskId) {
        return recommendationTrackingService.getTextTask(userId, taskId);
    }
}
