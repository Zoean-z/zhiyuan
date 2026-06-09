package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentMessageCreateRequest {

    @NotBlank
    private String content;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
