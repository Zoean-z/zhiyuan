package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;

public class AgentConversationRecordResponse {

    private Long id;
    private String title;
    private String status;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public AgentConversationRecordResponse(Long id,
                                           String title,
                                           String status,
                                           LocalDateTime lastMessageAt,
                                           Integer messageCount,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.lastMessageAt = lastMessageAt;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }

    public Long getId() {
        return id;
    }

    public String getTitle() {
        return title;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getLastMessageAt() {
        return lastMessageAt;
    }

    public Integer getMessageCount() {
        return messageCount;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
}
