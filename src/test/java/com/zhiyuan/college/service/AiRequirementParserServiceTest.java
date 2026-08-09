package com.zhiyuan.college.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyDouble;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.Test;

class AiRequirementParserServiceTest {

    private static final String RESPONSE_A = "{\"score\":620,\"subjectType\":\"PHYSICS\",\"candidateProvince\":\"浙江\"}";
    private static final String RESPONSE_B = "{\"score\":580,\"subjectType\":\"HISTORY\",\"candidateProvince\":\"湖南\"}";

    @Test
    void parseWithTrace_shouldNotReusePreviousRawResponseWhenNextAiCallFails() throws Exception {
        AiChatClient aiChatClient = mock(AiChatClient.class);
        when(aiChatClient.chat(anyString(), anyString(), anyDouble(), anyBoolean()))
                .thenReturn(RESPONSE_A)
                .thenThrow(new IllegalStateException("provider unavailable"));
        when(aiChatClient.getProvider()).thenReturn("test-provider");
        when(aiChatClient.getModel()).thenReturn("test-model");
        AiRequirementParserService service = service(aiChatClient, new ObjectMapper());

        AiRequirementParserService.ParseResult first = service.parseWithTrace("浙江物理620分");
        AiRequirementParserService.ParseResult second = service.parseWithTrace("湖南历史580分");

        assertEquals(RESPONSE_A, first.parseTrace().rawResponse());
        assertEquals("RULE_FALLBACK", second.parseTrace().parseMode());
        assertNull(second.parseTrace().rawResponse());
        assertTrue(second.parseTrace().errorMessage().contains("provider unavailable"));
    }

    @Test
    void parseWithTrace_shouldKeepConcurrentRawResponsesIsolated() throws Exception {
        AiChatClient aiChatClient = mock(AiChatClient.class);
        when(aiChatClient.chat(anyString(), anyString(), anyDouble(), anyBoolean())).thenAnswer(invocation -> {
            String prompt = invocation.getArgument(1, String.class);
            return prompt.contains("请求A") ? RESPONSE_A : RESPONSE_B;
        });
        when(aiChatClient.getProvider()).thenReturn("test-provider");
        when(aiChatClient.getModel()).thenReturn("test-model");

        ObjectMapper realMapper = new ObjectMapper();
        ObjectMapper coordinatingMapper = mock(ObjectMapper.class);
        CountDownLatch firstParseEntered = new CountDownLatch(1);
        CountDownLatch secondParseEntered = new CountDownLatch(1);
        when(coordinatingMapper.readTree(anyString())).thenAnswer(invocation -> {
            String raw = invocation.getArgument(0, String.class);
            if (RESPONSE_A.equals(raw)) {
                firstParseEntered.countDown();
                assertTrue(secondParseEntered.await(5, TimeUnit.SECONDS));
            } else {
                secondParseEntered.countDown();
            }
            return realMapper.readTree(raw);
        });
        AiRequirementParserService service = service(aiChatClient, coordinatingMapper);

        ExecutorService executor = Executors.newFixedThreadPool(2);
        try {
            Future<AiRequirementParserService.ParseResult> first = executor.submit(() -> service.parseWithTrace("请求A"));
            assertTrue(firstParseEntered.await(5, TimeUnit.SECONDS));
            Future<AiRequirementParserService.ParseResult> second = executor.submit(() -> service.parseWithTrace("请求B"));

            assertEquals(RESPONSE_A, first.get(5, TimeUnit.SECONDS).parseTrace().rawResponse());
            assertEquals(RESPONSE_B, second.get(5, TimeUnit.SECONDS).parseTrace().rawResponse());
        } finally {
            executor.shutdownNow();
        }
    }

    private AiRequirementParserService service(AiChatClient aiChatClient, ObjectMapper objectMapper) {
        RecommendationCacheService cacheService = mock(RecommendationCacheService.class);
        return new AiRequirementParserService(aiChatClient, objectMapper, cacheService, true);
    }
}
