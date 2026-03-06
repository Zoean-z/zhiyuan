package com.zhiyuan.college.model.dto;

public class LoginResponse {

    private String token;
    private String username;
    private Integer score;

    public LoginResponse(String token, String username, Integer score) {
        this.token = token;
        this.username = username;
        this.score = score;
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
}

