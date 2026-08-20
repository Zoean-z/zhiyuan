package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class FreeTextRecommendationTaskResponse {

    private Long taskId;
    private String requestId;
    private String status;
    private String sourceType;
    private String recommendationMode;
    private Integer resultCount;
    private Long durationMs;
    private String errorMessage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private ParsedRequirement parsedRequirement;
    private FreeTextRecommendationResponse result;

    public FreeTextRecommendationTaskResponse(Long taskId,
                                              String requestId,
                                              String status,
                                              String sourceType,
                                              String recommendationMode,
                                              Integer resultCount,
                                              Long durationMs,
                                              String errorMessage,
                                              LocalDateTime createdAt,
                                              LocalDateTime updatedAt,
                                              ParsedRequirement parsedRequirement,
                                              FreeTextRecommendationResponse result) {
        this.taskId = taskId;
        this.requestId = requestId;
        this.status = status;
        this.sourceType = sourceType;
        this.recommendationMode = recommendationMode;
        this.resultCount = resultCount;
        this.durationMs = durationMs;
        this.errorMessage = errorMessage;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.parsedRequirement = parsedRequirement;
        this.result = result;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getStatus() {
        return status;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getRecommendationMode() {
        return recommendationMode;
    }

    public Integer getResultCount() {
        return resultCount;
    }

    public Long getDurationMs() {
        return durationMs;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public ParsedRequirement getParsedRequirement() {
        return parsedRequirement;
    }

    public FreeTextRecommendationResponse getResult() {
        return result;
    }
}
