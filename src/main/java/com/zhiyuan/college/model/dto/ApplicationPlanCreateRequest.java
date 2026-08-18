package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class ApplicationPlanCreateRequest {

    @NotBlank
    private String planName;

    @NotBlank
    private String resultJson;

    private String aiSummary;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getResultJson() {
        return resultJson;
    }

    public void setResultJson(String resultJson) {
        this.resultJson = resultJson;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }
}
