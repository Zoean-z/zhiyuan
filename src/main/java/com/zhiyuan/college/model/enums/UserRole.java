package com.zhiyuan.college.model.enums;

public enum UserRole {
    USER,
    ADMIN;

    public static UserRole fromValue(String value) {
        if (value == null || value.isBlank()) {
            return USER;
        }
        return UserRole.valueOf(value.trim().toUpperCase());
    }
}
