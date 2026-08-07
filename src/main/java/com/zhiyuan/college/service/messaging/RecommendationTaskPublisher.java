package com.zhiyuan.college.service.messaging;

public interface RecommendationTaskPublisher {

    void publish(RecommendationTaskMessage message);
}
