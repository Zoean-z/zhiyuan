package com.zhiyuan.college.model.dto;

public record AiRuntimeConfigResponse(
        String provider,
        String baseUrl,
        String model,
        boolean apiKeyConfigured,
        String apiKeyMasked,
        String apiKeySource
) {
}
