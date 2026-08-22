package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequest {

    /** users.username is VARCHAR(64) and unique. */
    @NotBlank
    @Size(max = 64, message = "用户名长度不能超过 64")
    private String username;

    @NotBlank
    @Size(min = 6, max = 64, message = "密码长度需在 6-64 之间")
    private String password;

    @Min(0)
    @Max(750)
    private Integer score;

    private SubjectType subjectType;

    @Size(max = 32, message = "省份长度不能超过 32")
    private String examProvince;

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public String getExamProvince() {
        return examProvince;
    }

    public void setExamProvince(String examProvince) {
        this.examProvince = examProvince;
    }
}
