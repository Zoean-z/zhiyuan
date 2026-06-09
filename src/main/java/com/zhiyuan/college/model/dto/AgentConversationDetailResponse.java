package com.zhiyuan.college.model.dto;

import java.time.LocalDateTime;
import java.util.List;

public class AgentConversationDetailResponse {

    private Long id;
    private String title;
    private String status;
    private LocalDateTime lastMessageAt;
    private Integer messageCount;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private List<AgentMessageResponse> messages;

    public AgentConversationDetailResponse(Long id,
                                           String title,
                                           String status,
                                           LocalDateTime lastMessageAt,
                                           Integer messageCount,
                                           LocalDateTime createdAt,
                                           LocalDateTime updatedAt,
                                           List<AgentMessageResponse> messages) {
        this.id = id;
        this.title = title;
        this.status = status;
        this.lastMessageAt = lastMessageAt;
        this.messageCount = messageCount;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.messages = messages;
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

    public List<AgentMessageResponse> getMessages() {
        return messages;
    }
}
