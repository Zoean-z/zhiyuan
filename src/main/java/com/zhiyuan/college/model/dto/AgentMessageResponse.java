package com.zhiyuan.college.model.dto;

import com.fasterxml.jackson.databind.JsonNode;
import java.time.LocalDateTime;

public class AgentMessageResponse {

    private Long id;
    private String role;
    private String messageType;
    private String content;
    private String toolName;
    private JsonNode payload;
    private LocalDateTime createdAt;

    public AgentMessageResponse(Long id,
                                String role,
                                String messageType,
                                String content,
                                String toolName,
                                JsonNode payload,
                                LocalDateTime createdAt) {
        this.id = id;
        this.role = role;
        this.messageType = messageType;
        this.content = content;
        this.toolName = toolName;
        this.payload = payload;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getRole() {
        return role;
    }

    public String getMessageType() {
        return messageType;
    }

    public String getContent() {
        return content;
    }

    public String getToolName() {
        return toolName;
    }

    public JsonNode getPayload() {
        return payload;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
}
