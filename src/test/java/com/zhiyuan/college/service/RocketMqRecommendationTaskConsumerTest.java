package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.service.messaging.RecommendationTaskMessage;
import com.zhiyuan.college.service.messaging.RocketMqRecommendationTaskConsumer;
import java.nio.charset.StandardCharsets;
import org.apache.rocketmq.common.message.MessageExt;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class RocketMqRecommendationTaskConsumerTest {

    @Mock
    private AsyncRecommendationTaskRunner taskRunner;

    @Test
    void onMessage_shouldDeserializeAndForwardRetryCount() throws Exception {
        ObjectMapper objectMapper = new ObjectMapper();
        RocketMqRecommendationTaskConsumer consumer =
                new RocketMqRecommendationTaskConsumer(taskRunner, objectMapper);
        RecommendationTaskMessage payload = message();
        MessageExt messageExt = new MessageExt();
        messageExt.setBody(objectMapper.writeValueAsString(payload).getBytes(StandardCharsets.UTF_8));
        messageExt.setReconsumeTimes(2);

        consumer.onMessage(messageExt);

        ArgumentCaptor<RecommendationTaskMessage> messageCaptor =
                ArgumentCaptor.forClass(RecommendationTaskMessage.class);
        verify(taskRunner).processTextTask(messageCaptor.capture(), eq(2), eq(3));
        RecommendationTaskMessage forwarded = messageCaptor.getValue();
        assertEquals(11L, forwarded.taskId());
        assertEquals(7L, forwarded.userId());
        assertEquals("request-11", forwarded.requestId());
        assertEquals("推荐江苏的211学校", forwarded.request().getRequirementText());
    }

    private RecommendationTaskMessage message() {
        FreeTextRecommendationRequest request = new FreeTextRecommendationRequest();
        request.setRequirementText("推荐江苏的211学校");
        return new RecommendationTaskMessage(11L, 7L, "request-11", request);
    }
}
