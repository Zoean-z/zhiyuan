package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AgentMessageCreateRequest {

    @NotBlank
    private String content;

    private Long planId;

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public Long getPlanId() {
        return planId;
    }

    public void setPlanId(Long planId) {
        this.planId = planId;
    }
}
