package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import java.time.Duration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AsyncRecommendationTaskRunner {

    private static final Logger log = LoggerFactory.getLogger(AsyncRecommendationTaskRunner.class);

    private final RecommendationTrackingService recommendationTrackingService;
    private final FreeTextRecommendationService freeTextRecommendationService;
    private final HistoryService historyService;
    private final UserAccountMapper userAccountMapper;
    private final Duration runningTimeout;

    public AsyncRecommendationTaskRunner(RecommendationTrackingService recommendationTrackingService,
                                         FreeTextRecommendationService freeTextRecommendationService,
                                         HistoryService historyService,
                                         UserAccountMapper userAccountMapper,
                                         @Value("${messaging.rocketmq.running-timeout:5m}") Duration runningTimeout) {
        this.recommendationTrackingService = recommendationTrackingService;
        this.freeTextRecommendationService = freeTextRecommendationService;
        this.historyService = historyService;
        this.userAccountMapper = userAccountMapper;
        this.runningTimeout = runningTimeout;
    }

    public void processTextTask(RecommendationTaskMessage message,
                                int reconsumeTimes,
                                int maxReconsumeTimes) {
        if (!recommendationTrackingService.tryMarkTaskRunning(message.taskId(), runningTimeout)) {
            return;
        }

        long startedAt = System.currentTimeMillis();
        try {
            UserAccount user = message.userId() == null ? null : userAccountMapper.findByIdCompat(message.userId());
            FreeTextRecommendationService.ExecutionResult execution =
                    freeTextRecommendationService.execute(message.request(), message.requestId(), user);
            recommendationTrackingService.markTextTaskSuccess(
                    message.taskId(),
                    execution.parsedRequirement(),
                    execution.response(),
                    execution.parseTrace(),
                    System.currentTimeMillis() - startedAt
            );
            if (message.userId() != null) {
                saveHistoryBestEffort(message.userId(), message.request(), execution);
            }
        } catch (ResponseStatusException ex) {
            handleFailure(message.taskId(), reconsumeTimes, maxReconsumeTimes,
                    ex.getReason() == null ? ex.getMessage() : ex.getReason(), startedAt);
            throw ex;
        } catch (RuntimeException ex) {
            handleFailure(message.taskId(), reconsumeTimes, maxReconsumeTimes,
                    ex.getMessage() == null ? "Recommendation task execution failed" : ex.getMessage(), startedAt);
            throw ex;
        }
    }

    private void handleFailure(Long taskId,
                               int reconsumeTimes,
                               int maxReconsumeTimes,
                               String errorMessage,
                               long startedAt) {
        long durationMs = System.currentTimeMillis() - startedAt;
        if (maxReconsumeTimes > 0 && reconsumeTimes < maxReconsumeTimes) {
            recommendationTrackingService.markTaskPendingForRetry(taskId, errorMessage, durationMs);
            return;
        }
        recommendationTrackingService.markTaskFailed(taskId, errorMessage, durationMs);
    }

    private void saveHistoryBestEffort(Long userId,
                                       FreeTextRecommendationRequest request,
                                       FreeTextRecommendationService.ExecutionResult execution) {
        try {
            historyService.saveTextHistory(userId, request, execution.response());
        } catch (RuntimeException ex) {
            log.warn("Recommendation task succeeded but history persistence failed for user {}", userId, ex);
        }
    }
}
