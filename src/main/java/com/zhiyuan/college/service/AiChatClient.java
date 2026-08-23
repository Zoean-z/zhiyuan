package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.AiConnectionTestResponse;
import com.zhiyuan.college.model.dto.AiRuntimeConfigRequest;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.http.client.ClientHttpRequestFactory;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;
import org.springframework.web.server.ResponseStatusException;

@Component
public class AiChatClient {

    private static final Logger log = LoggerFactory.getLogger(AiChatClient.class);

    private final RestClient.Builder restClientBuilder;
    private final AiRuntimeConfigService runtimeConfigService;
    private final AiRuntimeConfigService.ResolvedAiConfig staticConfig;
    private final int retryMaxAttempts;
    private final long retryBackoffMillis;
    private final Integer connectTimeoutMillis;
    private final Integer readTimeoutMillis;

    @Autowired
    private ObjectMapper objectMapper;

    /**
     * Test friendly constructor: it deliberately leaves the request factory of the supplied builder
     * untouched so that mock servers can keep their own transport.
     */
    public AiChatClient(RestClient.Builder builder,
                        String baseUrl,
                        String apiKey,
                        String model,
                        int retryMaxAttempts,
                        long retryBackoffMillis) {
        this.restClientBuilder = builder;
        this.runtimeConfigService = null;
        this.staticConfig = new AiRuntimeConfigService.ResolvedAiConfig(
                "openai-compatible", baseUrl, model, apiKey);
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(0, retryBackoffMillis);
        this.connectTimeoutMillis = null;
        this.readTimeoutMillis = null;
    }

    @Autowired
    public AiChatClient(RestClient.Builder builder,
                        AiRuntimeConfigService runtimeConfigService,
                        @Value("${ai.qwen.retry.max-attempts:3}") int retryMaxAttempts,
                        @Value("${ai.qwen.retry.backoff-millis:150}") long retryBackoffMillis,
                        @Value("${ai.qwen.timeout.connect-millis:5000}") Integer connectTimeoutMillis,
                        @Value("${ai.qwen.timeout.read-millis:30000}") Integer readTimeoutMillis) {
        this.restClientBuilder = builder;
        this.runtimeConfigService = runtimeConfigService;
        this.staticConfig = null;
        this.retryMaxAttempts = Math.max(1, retryMaxAttempts);
        this.retryBackoffMillis = Math.max(0, retryBackoffMillis);
        this.connectTimeoutMillis = connectTimeoutMillis;
        this.readTimeoutMillis = readTimeoutMillis;
    }

    public String chat(String systemPrompt,
                       String userPrompt,
                       double temperature,
                       boolean jsonOutput) {
        return chat(systemPrompt, userPrompt, temperature, jsonOutput, null, null);
    }

    /**
     * Chat with optional {@code max_tokens} and {@code thinking} controls. Passing {@code null}
     * for either keeps the provider default, which lets the fallback advice path cap output
     * length and disable reasoning for fast, short answers.
     */
    public String chat(String systemPrompt,
                       String userPrompt,
                       double temperature,
                       boolean jsonOutput,
                       Integer maxTokens,
                       Boolean thinking) {
        AiRuntimeConfigService.ResolvedAiConfig config = resolveConfig();
        RestClient restClient = buildRestClient(config);
        Map<String, Object> requestBody = buildRequestBody(
                config.model(), systemPrompt, userPrompt, temperature, jsonOutput, false, maxTokens, thinking);

        Exception lastFailure = null;
        for (int attempt = 1; attempt <= retryMaxAttempts; attempt++) {
            try {
                return performChat(restClient, requestBody);
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
        return resolveConfig().model();
    }

    /**
     * Streams the chat completion (OpenAI-compatible SSE: lines prefixed with {@code data: },
     * deltas under {@code choices[0].delta.content}). Each text delta is delivered to
     * {@code onChunk}. Returns after the stream completes or fails.
     */
    public void chatStream(String systemPrompt,
                           String userPrompt,
                           double temperature,
                           boolean jsonOutput,
                           Consumer<String> onChunk) {
        chatStream(systemPrompt, userPrompt, temperature, jsonOutput, null, null, onChunk);
    }

    /**
     * Streaming variant with optional {@code max_tokens} and {@code thinking} controls.
     * A non-2xx upstream status (e.g. 401 bad API key, 500 server error) is surfaced as an
     * {@link IllegalStateException} so callers can fall back locally instead of showing a
     * silently empty stream.
     */
    public void chatStream(String systemPrompt,
                           String userPrompt,
                           double temperature,
                           boolean jsonOutput,
                           Integer maxTokens,
                           Boolean thinking,
                           Consumer<String> onChunk) {
        AiRuntimeConfigService.ResolvedAiConfig config = resolveConfig();
        RestClient restClient = buildRestClient(config);
        Map<String, Object> requestBody = buildRequestBody(
                config.model(), systemPrompt, userPrompt, temperature, jsonOutput, true, maxTokens, thinking);
        try {
            restClient.post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .exchange((request, response) -> {
                        if (!response.getStatusCode().is2xxSuccessful()) {
                            throw new IllegalStateException(
                                    "AI stream failed with status " + response.getStatusCode()
                            );
                        }
                        try (BufferedReader reader = new BufferedReader(
                                new InputStreamReader(response.getBody(), StandardCharsets.UTF_8))) {
                            String line;
                            while ((line = reader.readLine()) != null) {
                                if (!line.startsWith("data:")) {
                                    continue;
                                }
                                String data = line.substring(5).trim();
                                if ("[DONE]".equals(data)) {
                                    break;
                                }
                                JsonNode node = objectMapper.readTree(data);
                                JsonNode delta = node.path("choices").path(0).path("delta").path("content");
                                if (!delta.isMissingNode() && !delta.isNull()) {
                                    String text = delta.asText();
                                    // Keep pure whitespace/newline chunks: dropping them with
                                    // isBlank() would glue markdown headings, lists and tables.
                                    if (!text.isEmpty()) {
                                        onChunk.accept(text);
                                    }
                                }
                            }
                        }
                        return null;
                    });
        } catch (Exception ex) {
            log.warn("AI chat stream failed: {}", ex.getMessage());
            if (ex instanceof RuntimeException runtimeException) {
                throw runtimeException;
            }
            throw new IllegalStateException("AI chat stream failed", ex);
        }
    }

    public String getProvider() {
        return resolveConfig().provider();
    }

    public AiConnectionTestResponse testConnection(AiRuntimeConfigRequest request) {
        AiRuntimeConfigService.ResolvedAiConfig config = runtimeConfigService.resolveForTest(request);
        if (config.apiKey() == null || config.apiKey().isBlank()) {
            return new AiConnectionTestResponse(
                    false, "未配置 API Key，请填写后再检测", config.provider(), config.model(), 0);
        }

        long startedAt = System.nanoTime();
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", config.model());
        requestBody.put("messages", List.of(
                Map.of("role", "user", "content", "Reply OK.")
        ));
        requestBody.put("temperature", 0);
        requestBody.put("max_tokens", 8);
        try {
            JsonNode response = buildRestClient(config, 5000, 15000).post()
                    .uri("/chat/completions")
                    .body(requestBody)
                    .retrieve()
                    .body(JsonNode.class);
            JsonNode content = response == null
                    ? null
                    : response.path("choices").path(0).path("message").path("content");
            if (content == null || content.isMissingNode() || content.isNull()) {
                return connectionTestResult(false, "接口已响应，但返回格式不符合 OpenAI 兼容协议", config, startedAt);
            }
            return connectionTestResult(true, "连接成功，模型响应正常", config, startedAt);
        } catch (RestClientResponseException ex) {
            return connectionTestResult(
                    false, connectionTestFailureMessage(ex.getStatusCode().value()), config, startedAt);
        } catch (ResourceAccessException ex) {
            return connectionTestResult(false, "连接失败：接口不可达或响应超时", config, startedAt);
        } catch (Exception ex) {
            log.warn("AI connection test failed for provider {}: {}", config.provider(), ex.getMessage());
            return connectionTestResult(false, "连接失败：无法完成 OpenAI 兼容请求", config, startedAt);
        }
    }

    private String connectionTestFailureMessage(int statusCode) {
        return switch (statusCode) {
            case 400 -> "请求被上游拒绝（HTTP 400），请检查模型名称或接口兼容性";
            case 401, 403 -> "认证失败（HTTP " + statusCode + "），请检查 API Key 和账户权限";
            case 404 -> "接口不存在（HTTP 404），请检查兼容接口地址";
            case 429 -> "请求受限（HTTP 429），请检查额度或稍后重试";
            default -> statusCode >= 500
                    ? "上游服务异常（HTTP " + statusCode + "），请稍后重试"
                    : "连接失败：上游返回 HTTP " + statusCode;
        };
    }

    private AiConnectionTestResponse connectionTestResult(
            boolean available,
            String message,
            AiRuntimeConfigService.ResolvedAiConfig config,
            long startedAt) {
        long latencyMillis = Math.max(0, (System.nanoTime() - startedAt) / 1_000_000);
        return new AiConnectionTestResponse(
                available, message, config.provider(), config.model(), latencyMillis);
    }

    private Map<String, Object> buildRequestBody(String model,
                                                 String systemPrompt,
                                                 String userPrompt,
                                                 double temperature,
                                                 boolean jsonOutput,
                                                 boolean stream,
                                                 Integer maxTokens,
                                                 Boolean thinking) {
        Map<String, Object> requestBody = new LinkedHashMap<>();
        requestBody.put("model", model);
        requestBody.put("temperature", temperature);
        requestBody.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        requestBody.put("response_format", jsonOutput ? Map.of("type", "json_object") : Map.of("type", "text"));
        if (stream) {
            requestBody.put("stream", true);
        }
        if (maxTokens != null) {
            requestBody.put("max_tokens", maxTokens);
        }
        if (thinking != null) {
            requestBody.put("thinking", thinking);
        }
        return requestBody;
    }

    private AiRuntimeConfigService.ResolvedAiConfig resolveConfig() {
        return runtimeConfigService == null ? staticConfig : runtimeConfigService.resolve();
    }

    private RestClient buildRestClient(AiRuntimeConfigService.ResolvedAiConfig config) {
        return buildRestClient(config, connectTimeoutMillis, readTimeoutMillis);
    }

    private RestClient buildRestClient(AiRuntimeConfigService.ResolvedAiConfig config,
                                       Integer connectTimeout,
                                       Integer readTimeout) {
        RestClient.Builder configuredBuilder = restClientBuilder.clone()
                .baseUrl(config.baseUrl())
                .defaultHeader("Authorization", "Bearer " + config.apiKey())
                .defaultHeader("Content-Type", MediaType.APPLICATION_JSON_VALUE);
        if (connectTimeout != null && readTimeout != null) {
            configuredBuilder.requestFactory(buildRequestFactory(connectTimeout, readTimeout));
        }
        return configuredBuilder.build();
    }

    private static ClientHttpRequestFactory buildRequestFactory(int connectTimeoutMillis, int readTimeoutMillis) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Math.max(0, connectTimeoutMillis));
        requestFactory.setReadTimeout(Math.max(0, readTimeoutMillis));
        return requestFactory;
    }

    private String performChat(RestClient restClient, Map<String, Object> requestBody) {
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
