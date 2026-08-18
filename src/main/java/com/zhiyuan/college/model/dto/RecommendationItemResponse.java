package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.RecommendationMode;
import java.util.ArrayList;
import java.util.List;

public class RecommendationItemResponse {

    private RecommendationMode recommendationMode;
    private Long universityId;
    private String universityName;
    private String majorName;
    private String universityProvince;
    private String universityTier;
    private Boolean is985;
    private Boolean is211;
    private Boolean isDoubleFirstClass;
    private List<String> schoolTags;
    private String universityTags;
    private Integer cutoffScore;
    private Integer scoreGap;
    private Integer userRank;
    private Integer minRank;
    private Integer rankGap;
    private Integer admissionProbability;
    private String recommendationBasis;
    private String strategy;
    private String strategyLabel;
    private Integer riskScore;
    private List<String> matchReasons;
    private String explanation;

    public RecommendationItemResponse() {
    }

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
        this(RecommendationMode.SCHOOL_FIRST,
                null,
                universityName,
                null,
                null,
                null,
                null,
                null,
                null,
                new ArrayList<>(),
                null,
                cutoffScore,
                scoreGap,
                userRank,
                minRank,
                rankGap,
                admissionProbability,
                recommendationBasis,
                strategy,
                null,
                null,
                new ArrayList<>(),
                explanation);
    }

    public RecommendationItemResponse(RecommendationMode recommendationMode,
                                      Long universityId,
                                      String universityName,
                                      String majorName,
                                      String universityProvince,
                                      String universityTier,
                                      Boolean is985,
                                      Boolean is211,
                                      Boolean isDoubleFirstClass,
                                      List<String> schoolTags,
                                      String universityTags,
                                      Integer cutoffScore,
                                      Integer scoreGap,
                                      Integer userRank,
                                      Integer minRank,
                                      Integer rankGap,
                                      Integer admissionProbability,
                                      String recommendationBasis,
                                      String strategy,
                                      String strategyLabel,
                                      Integer riskScore,
                                      List<String> matchReasons,
                                      String explanation) {
        this.recommendationMode = recommendationMode;
        this.universityId = universityId;
        this.universityName = universityName;
        this.majorName = majorName;
        this.universityProvince = universityProvince;
        this.universityTier = universityTier;
        this.is985 = is985;
        this.is211 = is211;
        this.isDoubleFirstClass = isDoubleFirstClass;
        this.schoolTags = schoolTags == null ? new ArrayList<>() : new ArrayList<>(schoolTags);
        this.universityTags = universityTags;
        this.cutoffScore = cutoffScore;
        this.scoreGap = scoreGap;
        this.userRank = userRank;
        this.minRank = minRank;
        this.rankGap = rankGap;
        this.admissionProbability = admissionProbability;
        this.recommendationBasis = recommendationBasis;
        this.strategy = strategy;
        this.strategyLabel = strategyLabel;
        this.riskScore = riskScore;
        this.matchReasons = matchReasons == null ? new ArrayList<>() : new ArrayList<>(matchReasons);
        this.explanation = explanation;
    }

    public RecommendationMode getRecommendationMode() {
        return recommendationMode;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getMajorName() {
        return majorName;
    }

    public String getUniversityProvince() {
        return universityProvince;
    }

    public String getUniversityTier() {
        return universityTier;
    }

    public Boolean getIs985() {
        return is985;
    }

    public Boolean getIs211() {
        return is211;
    }

    public Boolean getIsDoubleFirstClass() {
        return isDoubleFirstClass;
    }

    public List<String> getSchoolTags() {
        return schoolTags;
    }

    public String getUniversityTags() {
        return universityTags;
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

    public String getStrategyLabel() {
        return strategyLabel;
    }

    public Integer getRiskScore() {
        return riskScore;
    }

    public List<String> getMatchReasons() {
        return matchReasons;
    }

    public String getExplanation() {
        return explanation;
    }

    public void setRecommendationMode(RecommendationMode recommendationMode) {
        this.recommendationMode = recommendationMode;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public void setUniversityProvince(String universityProvince) {
        this.universityProvince = universityProvince;
    }

    public void setUniversityTier(String universityTier) {
        this.universityTier = universityTier;
    }

    public void setIs985(Boolean is985) {
        this.is985 = is985;
    }

    public void setIs211(Boolean is211) {
        this.is211 = is211;
    }

    public void setIsDoubleFirstClass(Boolean doubleFirstClass) {
        isDoubleFirstClass = doubleFirstClass;
    }

    public void setSchoolTags(List<String> schoolTags) {
        this.schoolTags = schoolTags == null ? new ArrayList<>() : new ArrayList<>(schoolTags);
    }

    public void setUniversityTags(String universityTags) {
        this.universityTags = universityTags;
    }

    public void setCutoffScore(Integer cutoffScore) {
        this.cutoffScore = cutoffScore;
    }

    public void setScoreGap(Integer scoreGap) {
        this.scoreGap = scoreGap;
    }

    public void setUserRank(Integer userRank) {
        this.userRank = userRank;
    }

    public void setMinRank(Integer minRank) {
        this.minRank = minRank;
    }

    public void setRankGap(Integer rankGap) {
        this.rankGap = rankGap;
    }

    public void setAdmissionProbability(Integer admissionProbability) {
        this.admissionProbability = admissionProbability;
    }

    public void setRecommendationBasis(String recommendationBasis) {
        this.recommendationBasis = recommendationBasis;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public void setStrategyLabel(String strategyLabel) {
        this.strategyLabel = strategyLabel;
    }

    public void setRiskScore(Integer riskScore) {
        this.riskScore = riskScore;
    }

    public void setMatchReasons(List<String> matchReasons) {
        this.matchReasons = matchReasons == null ? new ArrayList<>() : new ArrayList<>(matchReasons);
    }

    public void setExplanation(String explanation) {
        this.explanation = explanation;
    }
}
