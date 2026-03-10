package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class HistoryRecordResponse {

    private Long id;
    private String queryType;
    private String queryContent;
    private LocalDateTime createdAt;

    public HistoryRecordResponse(Long id, String queryType, String queryContent, LocalDateTime createdAt) {
        this.id = id;
        this.queryType = queryType;
        this.queryContent = queryContent;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getQueryType() {
        return queryType;
    }

    public String getQueryContent() {
        return queryContent;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
