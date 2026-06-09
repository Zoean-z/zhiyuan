package com.zhiyuan.college.model.dto;

import java.util.List;

public class FreeTextRecommendationResponse {

    private String requestId;
    private ParsedRequirement parsed;
    private List<RecommendationItemResponse> recommendations;
    private String summary;
    private String finalAdvice;
    private String aiSummary;
    private List<String> tips;

    public FreeTextRecommendationResponse() {
    }

    public FreeTextRecommendationResponse(String requestId,
                                          ParsedRequirement parsed,
                                          List<RecommendationItemResponse> recommendations,
                                          String summary,
                                          String finalAdvice,
                                          String aiSummary,
                                          List<String> tips) {
        this.requestId = requestId;
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

    public void setParsed(ParsedRequirement parsed) {
        this.parsed = parsed;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public List<RecommendationItemResponse> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<RecommendationItemResponse> recommendations) {
        this.recommendations = recommendations;
    }

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getFinalAdvice() {
        return finalAdvice;
    }

    public void setFinalAdvice(String finalAdvice) {
        this.finalAdvice = finalAdvice;
    }

    public String getAiSummary() {
        return aiSummary;
    }

    public void setAiSummary(String aiSummary) {
        this.aiSummary = aiSummary;
    }

    public List<String> getTips() {
        return tips;
    }

    public void setTips(List<String> tips) {
        this.tips = tips;
    }
}
