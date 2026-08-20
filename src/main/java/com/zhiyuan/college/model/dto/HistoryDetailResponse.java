package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class HistoryDetailResponse {

    private Long id;
    private String queryType;
    private String queryContent;
    private String resultJson;
    private LocalDateTime createdAt;

    public HistoryDetailResponse(Long id, String queryType, String queryContent, String resultJson, LocalDateTime createdAt) {
        this.id = id;
        this.queryType = queryType;
        this.queryContent = queryContent;
        this.resultJson = resultJson;
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

    public String getResultJson() {
        return resultJson;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
