package com.zhiyuan.college.service.messaging;

import com.zhiyuan.college.service.AsyncRecommendationTaskRunner;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.rocketmq.enabled", havingValue = "false", matchIfMissing = true)
public class LocalRecommendationTaskPublisher implements RecommendationTaskPublisher {

    private final AsyncRecommendationTaskRunner taskRunner;

    public LocalRecommendationTaskPublisher(AsyncRecommendationTaskRunner taskRunner) {
        this.taskRunner = taskRunner;
    }

    @Override
    public void publish(RecommendationTaskMessage message) {
        taskRunner.processTextTask(message, 0, 0);
    }
}
