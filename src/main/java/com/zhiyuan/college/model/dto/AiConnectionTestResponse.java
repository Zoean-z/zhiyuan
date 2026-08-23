package com.zhiyuan.college.model.dto;

public record AiConnectionTestResponse(
        boolean available,
        String message,
        String provider,
        String model,
        long latencyMillis
) {
}
