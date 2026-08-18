package com.zhiyuan.college.model.dto;

public record EmailVerificationCodeResponse(String message, long expiresInSeconds, long resendAfterSeconds) {
}
