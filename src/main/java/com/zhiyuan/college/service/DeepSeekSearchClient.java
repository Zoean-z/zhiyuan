package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CancellationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class DeepSeekSearchClient {
    private final ObjectMapper mapper;
    private final AiRuntimeConfigService runtimeConfig;
    private final String apiKey;
    private final URI base;
    private final HttpClient http;

    @Autowired
    public DeepSeekSearchClient(ObjectMapper mapper, AiRuntimeConfigService runtimeConfig,
            @Value("${ai.search.api-key:}") String apiKey,
            @Value("${ai.search.base-url:https://api.deepseek.com}") String baseUrl) {
        this(mapper, runtimeConfig, apiKey, baseUrl, HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(4)).followRedirects(HttpClient.Redirect.NEVER).build());
    }

    DeepSeekSearchClient(ObjectMapper mapper, AiRuntimeConfigService config, String key, String url, HttpClient http) {
        this.mapper = mapper;
        this.runtimeConfig = config;
        this.apiKey = key == null ? "" : key.trim();
        this.base = URI.create(url.replaceAll("/+$", ""));
        this.http = http;
    }

    public boolean isConfigured() { return !resolveKey().isBlank(); }

    private String resolveKey() {
        if (!apiKey.isBlank()) return apiKey;
        if (runtimeConfig != null && "api.deepseek.com".equalsIgnoreCase(base.getHost())) {
            var config = runtimeConfig.resolve();
            if ("api.deepseek.com".equalsIgnoreCase(URI.create(config.baseUrl()).getHost())) {
                return config.apiKey() == null ? "" : config.apiKey();
            }
        }
        return "";
    }

    public JsonNode search(String query, long deadlineNanos) {
        String bounded = query.codePointCount(0, query.length()) > 128
                ? query.substring(0, query.offsetByCodePoints(0, 128)) : query;
        return post("/v1/tools/search", Map.of("query", bounded, "count", 5, "need_content", true), deadlineNanos);
    }

    public JsonNode fetch(String url, long deadlineNanos) {
        if (!isPublicUrl(url)) throw new IllegalArgumentException("非公开网页地址，不抓取");
        return post("/v1/tools/fetch", Map.of("url", url), deadlineNanos);
    }

    public static boolean isPublicUrl(String value) {
        try {
            URI uri = URI.create(value);
            String h = uri.getHost();
            if (h == null || uri.getUserInfo() != null || value.length() > 8192
                    || !("https".equals(uri.getScheme()) || "http".equals(uri.getScheme()))) return false;
            h = h.toLowerCase(java.util.Locale.ROOT);
            // Evidence links must be public DNS names, not IP literals or local services.
            return h.contains(".") && !h.matches("[0-9.]+") && !h.contains(":")
                    && !h.endsWith(".localhost") && !h.endsWith(".local") && !h.endsWith(".internal");
        } catch (IllegalArgumentException ex) { return false; }
    }

    private JsonNode post(String path, Map<String, Object> body, long deadlineNanos) {
        String key = resolveKey();
        if (key.isBlank()) throw new IllegalStateException("未配置 DeepSeek 搜索凭据");
        if (Thread.currentThread().isInterrupted()) throw new CancellationException("分析已停止");
        long remaining = deadlineNanos - System.nanoTime();
        if (remaining <= 0) throw new IllegalStateException("联网检索时间预算已用尽");
        try {
            HttpRequest request = HttpRequest.newBuilder(URI.create(base + path))
                    .timeout(Duration.ofNanos(Math.min(remaining, Duration.ofSeconds(7).toNanos())))
                    .header("Authorization", "Bearer " + key).header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(mapper.writeValueAsString(body))).build();
            HttpResponse<String> response = http.send(request, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                String reason = switch (response.statusCode()) {
                    case 401, 403 -> "搜索凭据无效或无访问权限";
                    case 402 -> "搜索账户余额不足";
                    case 429 -> "搜索服务限流";
                    default -> "搜索服务暂不可用（HTTP " + response.statusCode() + "）";
                };
                throw new IllegalStateException(reason);
            }
            if (response.body().length() > 2_000_000) throw new IllegalStateException("网页资料过大");
            return mapper.readTree(response.body());
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new CancellationException("分析已停止");
        } catch (java.io.IOException ex) {
            throw new IllegalStateException("联网请求失败或超时", ex);
        }
    }
}
