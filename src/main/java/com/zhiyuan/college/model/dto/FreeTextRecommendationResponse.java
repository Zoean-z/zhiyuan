package com.zhiyuan.college.model.dto;

import java.util.List;

public class FreeTextRecommendationResponse {

    private ParsedRequirement parsed;
    private List<RecommendationItemResponse> recommendations;
    private String summary;
    private String finalAdvice;
    private String aiSummary;
    private List<String> tips;

    public FreeTextRecommendationResponse(ParsedRequirement parsed,
                                          List<RecommendationItemResponse> recommendations,
                                          String summary,
                                          String finalAdvice,
                                          String aiSummary,
                                          List<String> tips) {
        this.parsed = parsed;
        this.recommendations = recommendations;
        this.summary = summary;
        this.finalAdvice = finalAdvice;
        this.aiSummary = aiSummary;
        this.tips = tips;
    }

    public ParsedRequirement getParsed() {
        return parsed;
    }

    public List<RecommendationItemResponse> getRecommendations() {
        return recommendations;
    }

    public String getSummary() {
        return summary;
    }

    public String getFinalAdvice() {
        return finalAdvice;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public List<String> getTips() {
        return tips;
    }
}
