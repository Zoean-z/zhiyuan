package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import com.zhiyuan.college.service.messaging.RocketMqRecommendationTaskPublisher;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.client.producer.SendStatus;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;

@ExtendWith(MockitoExtension.class)
class RocketMqRecommendationTaskPublisherTest {

    @Mock
    private RocketMQTemplate rocketMQTemplate;

    @Mock
    private SendResult sendResult;

    private RocketMqRecommendationTaskPublisher publisher;

    @BeforeEach
    void setUp() {
        publisher = new RocketMqRecommendationTaskPublisher(
                rocketMQTemplate,
                new ObjectMapper(),
                "recommendation-task-topic",
                "free-text",
                3000L
        );
    }

    @Test
    void publish_shouldSendMessageWithConfiguredDestination() {
        when(sendResult.getSendStatus()).thenReturn(SendStatus.SEND_OK);
        when(rocketMQTemplate.syncSend(
                eq("recommendation-task-topic:free-text"),
                any(Message.class),
                eq(3000L)
        )).thenReturn(sendResult);

        publisher.publish(message());

        verify(rocketMQTemplate).syncSend(
                eq("recommendation-task-topic:free-text"),
                any(Message.class),
                eq(3000L)
        );
    }

    @Test
    void publish_shouldRejectNonOkSendResult() {
        when(sendResult.getSendStatus()).thenReturn(SendStatus.FLUSH_DISK_TIMEOUT);
        when(rocketMQTemplate.syncSend(any(), any(Message.class), eq(3000L))).thenReturn(sendResult);

        assertThrows(IllegalStateException.class, () -> publisher.publish(message()));
    }

    private RecommendationTaskMessage message() {
        FreeTextRecommendationRequest request = new FreeTextRecommendationRequest();
        request.setRequirementText("推荐江苏的211学校");
        return new RecommendationTaskMessage(11L, 7L, "request-11", request);
    }
}
