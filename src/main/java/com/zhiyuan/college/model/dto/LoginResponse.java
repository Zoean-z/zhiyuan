package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;

public class LoginResponse {

    private String token;
    private String username;
    private Integer score;
    private SubjectType subjectType;
    private String examProvince;

    public LoginResponse(String token, String username, Integer score, SubjectType subjectType, String examProvince) {
        this.token = token;
        this.username = username;
        this.score = score;
        this.subjectType = subjectType;
        this.examProvince = examProvince;
    }

    public String getToken() {
        return token;
    }

    public String getUsername() {
        return username;
    }

    public Integer getScore() {
        return score;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public String getExamProvince() {
        return examProvince;
    }
}
