package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.RecommendationMode;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public class RecommendationRequest {

    @Min(0)
    @Max(750)
    private Integer score;

    @NotBlank
    private String province;

    private SubjectType subjectType;
    private RecommendationMode recommendationMode;
    private String majorKeyword;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public RecommendationMode getRecommendationMode() {
        return recommendationMode;
    }

    public void setRecommendationMode(RecommendationMode recommendationMode) {
        this.recommendationMode = recommendationMode;
    }

    public String getMajorKeyword() {
        return majorKeyword;
    }

    public void setMajorKeyword(String majorKeyword) {
        this.majorKeyword = majorKeyword;
    }
}
