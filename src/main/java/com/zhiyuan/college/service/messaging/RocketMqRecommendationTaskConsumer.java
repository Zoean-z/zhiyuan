package com.zhiyuan.college.service.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.service.AsyncRecommendationTaskRunner;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.apache.rocketmq.spring.annotation.RocketMQMessageListener;
import org.apache.rocketmq.spring.core.RocketMQListener;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.rocketmq.enabled", havingValue = "true")
@RocketMQMessageListener(
        topic = "${messaging.rocketmq.topic}",
        selectorExpression = "${messaging.rocketmq.tag}",
        consumerGroup = "${messaging.rocketmq.consumer-group}",
        maxReconsumeTimes = 3
)
public class RocketMqRecommendationTaskConsumer implements RocketMQListener<MessageExt> {

    private final AsyncRecommendationTaskRunner taskRunner;
    private final ObjectMapper objectMapper;

    public RocketMqRecommendationTaskConsumer(
            AsyncRecommendationTaskRunner taskRunner,
            ObjectMapper objectMapper) {
        this.taskRunner = taskRunner;
        this.objectMapper = objectMapper;
    }

    @Override
    public void onMessage(MessageExt messageExt) {
        RecommendationTaskMessage message = parseMessage(messageExt);
        taskRunner.processTextTask(message, messageExt.getReconsumeTimes(), 3);
    }

    private RecommendationTaskMessage parseMessage(MessageExt messageExt) {
        try {
            String payload = new String(messageExt.getBody(), StandardCharsets.UTF_8);
            return objectMapper.readValue(payload, RecommendationTaskMessage.class);
        } catch (Exception ex) {
            throw new IllegalStateException("Invalid RocketMQ recommendation task message", ex);
        }
    }
}
