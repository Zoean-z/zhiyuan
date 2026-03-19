package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.ArrayList;
import java.util.List;

public class ParsedRequirement {

    private Integer score;
    private RecommendationMode recommendationMode;
    private List<String> schoolLevels = new ArrayList<>();
    private List<String> schoolTypes = new ArrayList<>();
    private List<String> provinces = new ArrayList<>();
    private List<String> majorKeywords = new ArrayList<>();
    private List<String> normalizedMajors = new ArrayList<>();
    private String riskPreference;
    private List<String> unrecognizedPreferences = new ArrayList<>();
    private String candidateProvince;
    private String schoolProvince;
    private SubjectType subjectType;
    private StrategyType strategy;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public RecommendationMode getRecommendationMode() {
        return recommendationMode;
    }

    public void setRecommendationMode(RecommendationMode recommendationMode) {
        this.recommendationMode = recommendationMode;
    }

    public List<String> getSchoolLevels() {
        return schoolLevels;
    }

    public void setSchoolLevels(List<String> schoolLevels) {
        this.schoolLevels = schoolLevels == null ? new ArrayList<>() : new ArrayList<>(schoolLevels);
    }

    public List<String> getProvinces() {
        return provinces;
    }

    public void setProvinces(List<String> provinces) {
        this.provinces = provinces == null ? new ArrayList<>() : new ArrayList<>(provinces);
    }

    public List<String> getSchoolTypes() {
        return schoolTypes;
    }

    public void setSchoolTypes(List<String> schoolTypes) {
        this.schoolTypes = schoolTypes == null ? new ArrayList<>() : new ArrayList<>(schoolTypes);
    }

    public List<String> getMajorKeywords() {
        return majorKeywords;
    }

    public void setMajorKeywords(List<String> majorKeywords) {
        this.majorKeywords = majorKeywords == null ? new ArrayList<>() : new ArrayList<>(majorKeywords);
    }

    public List<String> getNormalizedMajors() {
        return normalizedMajors;
    }

    public void setNormalizedMajors(List<String> normalizedMajors) {
        this.normalizedMajors = normalizedMajors == null ? new ArrayList<>() : new ArrayList<>(normalizedMajors);
    }

    public String getRiskPreference() {
        return riskPreference;
    }

    public void setRiskPreference(String riskPreference) {
        this.riskPreference = riskPreference;
    }

    public List<String> getUnrecognizedPreferences() {
        return unrecognizedPreferences;
    }

    public void setUnrecognizedPreferences(List<String> unrecognizedPreferences) {
        this.unrecognizedPreferences = unrecognizedPreferences == null ? new ArrayList<>() : new ArrayList<>(unrecognizedPreferences);
    }

    public String getCandidateProvince() {
        return candidateProvince;
    }

    public void setCandidateProvince(String candidateProvince) {
        this.candidateProvince = candidateProvince;
    }

    public String getSchoolProvince() {
        return schoolProvince;
    }

    public void setSchoolProvince(String schoolProvince) {
        this.schoolProvince = schoolProvince;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public StrategyType getStrategy() {
        return strategy;
    }

    public void setStrategy(StrategyType strategy) {
        this.strategy = strategy;
    }
}
