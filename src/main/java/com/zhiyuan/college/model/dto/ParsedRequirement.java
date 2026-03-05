package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.StrategyType;

public class ParsedRequirement {

    private Integer score;
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
