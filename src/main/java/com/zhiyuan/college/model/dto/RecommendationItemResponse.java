package com.zhiyuan.college.model.dto;

public class RecommendationItemResponse {

    private String universityName;
    private Integer cutoffScore;
    private Integer scoreGap;
    private Integer admissionProbability;
    private String strategy;
    private String explanation;

    public RecommendationItemResponse(String universityName,
                                      Integer cutoffScore,
                                      Integer scoreGap,
                                      Integer admissionProbability,
                                      String strategy,
                                      String explanation) {
        this.universityName = universityName;
        this.cutoffScore = cutoffScore;
        this.scoreGap = scoreGap;
        this.admissionProbability = admissionProbability;
        this.strategy = strategy;
        this.explanation = explanation;
    }

    public String getUniversityName() {
        return universityName;
    }

    public Integer getCutoffScore() {
        return cutoffScore;
    }

    public Integer getScoreGap() {
        return scoreGap;
    }

    public Integer getAdmissionProbability() {
        return admissionProbability;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getExplanation() {
        return explanation;
    }
}
