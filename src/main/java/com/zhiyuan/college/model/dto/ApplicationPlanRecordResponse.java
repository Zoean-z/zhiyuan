package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class ApplicationPlanRecordResponse {

    private Long id;
    private String planName;
    private String sourceType;
    private String sourceQuery;
    private LocalDateTime createdAt;

    public ApplicationPlanRecordResponse(Long id,
                                         String planName,
                                         String sourceType,
                                         String sourceQuery,
                                         LocalDateTime createdAt) {
        this.id = id;
        this.planName = planName;
        this.sourceType = sourceType;
        this.sourceQuery = sourceQuery;
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

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
