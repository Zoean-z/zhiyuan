package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.UserRole;

public class LoginResponse {

    private String token;
    private String username;
    private Integer score;
    private SubjectType subjectType;
    private String examProvince;
    private UserRole role;

    public LoginResponse(String token, String username, Integer score, SubjectType subjectType, String examProvince, UserRole role) {
        this.token = token;
        this.username = username;
        this.score = score;
        this.subjectType = subjectType;
        this.examProvince = examProvince;
        this.role = role;
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

    public UserRole getRole() {
        return role;
    }
}
