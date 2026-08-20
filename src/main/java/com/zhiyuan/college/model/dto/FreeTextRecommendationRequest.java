package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class FreeTextRecommendationRequest {

    @NotBlank
    private String requirementText;

    public String getRequirementText() {
        return requirementText;
    }

    public void setRequirementText(String requirementText) {
        this.requirementText = requirementText;
    }
}
