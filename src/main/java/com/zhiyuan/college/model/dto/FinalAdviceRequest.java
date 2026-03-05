package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;

public class FinalAdviceRequest {

    @NotNull
    @Min(0)
    @Max(750)
    private Integer score;

    @NotBlank
    private String province;

    @NotNull
    private SubjectType subjectType;

    @NotBlank
    private String strategy;

    private List<String> preferredUniversities;

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

    public String getStrategy() {
        return strategy;
    }

    public void setStrategy(String strategy) {
        this.strategy = strategy;
    }

    public List<String> getPreferredUniversities() {
        return preferredUniversities;
    }

    public void setPreferredUniversities(List<String> preferredUniversities) {
        this.preferredUniversities = preferredUniversities;
    }
}
