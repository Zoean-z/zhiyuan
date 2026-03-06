package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import java.util.List;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.server.ResponseStatusException;

@Component
public class QwenAiClient {

    private final RestClient restClient;
    private final String model;

    public QwenAiClient(RestClient.Builder builder,
                        @Value("${ai.qwen.base-url}") String baseUrl,
                        @Value("${ai.qwen.api-key}") String apiKey,
                        @Value("${ai.qwen.model}") String model) {
        this.restClient = builder
                .baseUrl(baseUrl)
                .defaultHeader("Authorization", "Bearer " + apiKey)
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE)
                .build();
        this.model = model;
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

        JsonNode response = restClient.post()
                .uri("/chat/completions")
                .body(requestBody)
                .retrieve()
                .body(JsonNode.class);

        if (response == null) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "AI response is empty");
        }

        JsonNode contentNode = response.path("choices").path(0).path("message").path("content");
        if (contentNode.isMissingNode() || contentNode.asText().isBlank()) {
            throw new ResponseStatusException(org.springframework.http.HttpStatus.BAD_GATEWAY, "AI response content is empty");
        }
        return contentNode.asText();
    }
}

