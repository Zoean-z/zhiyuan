package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class ApplicationPlanDetailResponse {

    private Long id;
    private String planName;
    private String sourceType;
    private String sourceQuery;
    private String resultJson;
    private String aiSummary;
    private LocalDateTime createdAt;

    public ApplicationPlanDetailResponse(Long id,
                                         String planName,
                                         String sourceType,
                                         String sourceQuery,
                                         String resultJson,
                                         String aiSummary,
                                         LocalDateTime createdAt) {
        this.id = id;
        this.planName = planName;
        this.sourceType = sourceType;
        this.sourceQuery = sourceQuery;
        this.resultJson = resultJson;
        this.aiSummary = aiSummary;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getPlanName() {
        return planName;
    }

    public String getSourceType() {
        return sourceType;
    }

    public String getSourceQuery() {
        return sourceQuery;
    }

    public String getResultJson() {
        return resultJson;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
