package com.zhiyuan.college.model.dto;

public class RecommendationItemResponse {

    private String universityName;
    private Integer cutoffScore;
    private Integer scoreGap;
    private Integer userRank;
    private Integer minRank;
    private Integer rankGap;
    private Integer admissionProbability;
    private String recommendationBasis;
    private String strategy;
    private String explanation;

    public RecommendationItemResponse(String universityName,
                                      Integer cutoffScore,
                                      Integer scoreGap,
                                      Integer userRank,
                                      Integer minRank,
                                      Integer rankGap,
                                      Integer admissionProbability,
                                      String recommendationBasis,
                                      String strategy,
                                      String explanation) {
        this.universityName = universityName;
        this.cutoffScore = cutoffScore;
        this.scoreGap = scoreGap;
        this.userRank = userRank;
        this.minRank = minRank;
        this.rankGap = rankGap;
        this.admissionProbability = admissionProbability;
        this.recommendationBasis = recommendationBasis;
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

    public Integer getUserRank() {
        return userRank;
    }

    public Integer getMinRank() {
        return minRank;
    }

    public Integer getRankGap() {
        return rankGap;
    }

    public Integer getAdmissionProbability() {
        return admissionProbability;
    }

    public String getRecommendationBasis() {
        return recommendationBasis;
    }

    public String getStrategy() {
        return strategy;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
