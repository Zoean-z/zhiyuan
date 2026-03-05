package com.zhiyuan.college.model.dto;

import java.util.List;

public class FinalAdviceResponse {

    private String resolvedStrategy;
    private List<String> recommendedUniversities;
    private String finalAdvice;
    private String aiSummary;

    public FinalAdviceResponse(String resolvedStrategy,
                               List<String> recommendedUniversities,
                               String finalAdvice,
                               String aiSummary) {
        this.resolvedStrategy = resolvedStrategy;
        this.recommendedUniversities = recommendedUniversities;
        this.finalAdvice = finalAdvice;
        this.aiSummary = aiSummary;
    }

    public String getResolvedStrategy() {
        return resolvedStrategy;
    }

    public List<String> getRecommendedUniversities() {
        return recommendedUniversities;
    }

    public String getFinalAdvice() {
        return finalAdvice;
    }

    public String getAiSummary() {
        return aiSummary;
    }
}
