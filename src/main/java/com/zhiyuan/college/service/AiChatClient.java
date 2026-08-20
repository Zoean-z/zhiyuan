package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AiChatClient {

    private final RestClient restClient;
    private final String model;
    private final String provider;
    private final int retryMaxAttempts;
    private final long retryBackoffMillis;

    public AiChatClient(RestClient.Builder builder,
                        @Value("${ai.qwen.base-url}") String baseUrl,
                        @Value("${ai.qwen.api-key}") String apiKey,
                        @Value("${ai.qwen.model}") String model,
                        @Value("${ai.qwen.retry.max-attempts:3}") int retryMaxAttempts,
                        @Value("${ai.qwen.retry.backoff-millis:150}") long retryBackoffMillis) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
        this.provider = "openai-compatible";
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(0, retryBackoffMillis);
    }

    public String chat(String systemPrompt,
                       String userPrompt,
                       double temperature,
                       boolean jsonOutput) {
        Map<String, Object> requestBody = Map.of(
                "model", model,
                "temperature", temperature,
                "messages", List.of(
                        Map.of("role", "system", "content", systemPrompt),
                        Map.of("role", "user", "content", userPrompt)
                ),
                "response_format", jsonOutput ? Map.of("type", "json_object") : Map.of("type", "text")
        );

        Exception lastFailure = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                return performChat(requestBody);
            } catch (Exception ex) {
                lastFailure = ex;
                if (attempt >= retryMaxAttempts || !isRetryable(ex)) {
                    break;
                }
                sleepBeforeRetry();
            }
        }

        if (lastFailure instanceof RuntimeException runtimeException) {
            throw runtimeException;
        }
        throw new IllegalStateException("AI chat failed after retries", lastFailure);
    }

    public String getModel() {
        return model;
    }

    public String getProvider() {
        return provider;
    }

    private String performChat(Map<String, Object> requestBody) {
        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response is empty");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_GATEWAY, "AI response content is empty");
        }
        return contentNode.asText();
    }

    private boolean isRetryable(Exception ex) {
        if (ex instanceof ResourceAccessException) {
            return true;
        }
        if (ex instanceof ResponseStatusException responseStatusException) {
            return isRetryableStatus(responseStatusException.getStatusCode());
        }
        if (ex instanceof RestClientResponseException restClientResponseException) {
            return isRetryableStatus(HttpStatusCode.valueOf(restClientResponseException.getStatusCode().value()));
        }
        if (ex instanceof RestClientException restClientException) {
            return restClientException.getCause() instanceof ResourceAccessException;
        }
        return false;
    }

    private boolean isRetryableStatus(HttpStatusCode statusCode) {
        return statusCode.value() == 429
                || statusCode.value() == 502
                || statusCode.value() == 503
                || statusCode.value() == 504;
    }

    private void sleepBeforeRetry() {
        if (retryBackoffMillis <= 0) {
            return;
        }
        try {
            Thread.sleep(retryBackoffMillis);
        } catch (InterruptedException interruptedException) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("AI retry interrupted", interruptedException);
        }
    }
}
