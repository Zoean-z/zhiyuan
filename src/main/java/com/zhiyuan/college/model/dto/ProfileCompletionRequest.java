package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.ElectiveSubject;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProfileCompletionRequest {

    @NotBlank
    private String examProvince;

    @NotNull
    private SubjectType subjectType;

    @NotNull
    @Min(0)
    @Max(750)
    private Integer score;

    @NotNull
    @Size(min = 2, max = 2)
    private List<@NotNull ElectiveSubject> electiveSubjects;

    public String getExamProvince() {
        return examProvince;
    }

    public void setExamProvince(String examProvince) {
        this.examProvince = examProvince;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public List<ElectiveSubject> getElectiveSubjects() {
        return electiveSubjects;
    }

    public void setElectiveSubjects(List<ElectiveSubject> electiveSubjects) {
        this.electiveSubjects = electiveSubjects;
    }
}
