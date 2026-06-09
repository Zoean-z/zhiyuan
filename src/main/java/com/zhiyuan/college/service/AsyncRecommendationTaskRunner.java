package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AsyncRecommendationTaskRunner {

    private final RecommendationTrackingService recommendationTrackingService;
    private final FreeTextRecommendationService freeTextRecommendationService;
    private final HistoryService historyService;
    private final UserAccountMapper userAccountMapper;

    public AsyncRecommendationTaskRunner(RecommendationTrackingService recommendationTrackingService,
                                         FreeTextRecommendationService freeTextRecommendationService,
                                         HistoryService historyService,
                                         UserAccountMapper userAccountMapper) {
        this.recommendationTrackingService = recommendationTrackingService;
        this.freeTextRecommendationService = freeTextRecommendationService;
        this.historyService = historyService;
        this.userAccountMapper = userAccountMapper;
    }

    @Async("recommendationTaskExecutor")
    public void processTextTask(Long taskId,
                                Long userId,
                                FreeTextRecommendationRequest request,
                                String requestId) {
        long startedAt = System.currentTimeMillis();
        recommendationTrackingService.markTaskRunning(taskId);
        try {
            UserAccount user = userId == null ? null : userAccountMapper.findByIdCompat(userId);
            FreeTextRecommendationService.ExecutionResult execution =
                    freeTextRecommendationService.execute(request, requestId, user);
            recommendationTrackingService.markTextTaskSuccess(
                    taskId,
                    execution.parsedRequirement(),
                    execution.response(),
                    execution.parseTrace(),
                    System.currentTimeMillis() - startedAt
            );
            if (userId != null) {
                historyService.saveTextHistory(userId, request, execution.response());
            }
        } catch (ResponseStatusException ex) {
            recommendationTrackingService.markTaskFailed(
                    taskId,
                    ex.getReason() == null ? ex.getMessage() : ex.getReason(),
                    System.currentTimeMillis() - startedAt
            );
        } catch (Exception ex) {
            recommendationTrackingService.markTaskFailed(
                    taskId,
                    ex.getMessage() == null ? "Async recommendation task failed" : ex.getMessage(),
                    System.currentTimeMillis() - startedAt
            );
        }
    }
}
