package com.zhiyuan.college.service.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.apache.rocketmq.spring.support.RocketMQHeaders;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;

@Component
@ConditionalOnProperty(name = "messaging.rocketmq.enabled", havingValue = "true")
public class RocketMqRecommendationTaskPublisher implements RecommendationTaskPublisher {

    private final RocketMQTemplate rocketMQTemplate;
    private final ObjectMapper objectMapper;
    private final String destination;
    private final long sendTimeoutMs;

    public RocketMqRecommendationTaskPublisher(RocketMQTemplate rocketMQTemplate,
                                               ObjectMapper objectMapper,
                                               @Value("${messaging.rocketmq.topic}") String topic,
                                               @Value("${messaging.rocketmq.tag}") String tag,
                                               @Value("${messaging.rocketmq.send-timeout-ms:3000}") long sendTimeoutMs) {
        this.rocketMQTemplate = rocketMQTemplate;
        this.objectMapper = objectMapper;
        this.destination = topic + ":" + tag;
        this.sendTimeoutMs = sendTimeoutMs;
    }

    @Override
    public void publish(RecommendationTaskMessage message) {
        String payload = toJson(message);
        SendResult sendResult = rocketMQTemplate.syncSend(
                destination,
                MessageBuilder.withPayload(payload)
                        .setHeader(RocketMQHeaders.KEYS, message.requestId())
                        .build(),
                sendTimeoutMs
        );
        if (sendResult == null || sendResult.getSendStatus() != SendStatus.SEND_OK) {
            throw new IllegalStateException("RocketMQ recommendation task publish failed");
        }
    }

    private String toJson(RecommendationTaskMessage message) {
        try {
            return objectMapper.writeValueAsString(message);
        } catch (JsonProcessingException ex) {
            throw new IllegalStateException("Failed to serialize recommendation task message", ex);
        }
    }
}
