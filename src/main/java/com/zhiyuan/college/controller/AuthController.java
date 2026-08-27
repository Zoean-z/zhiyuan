package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.LoginRequest;
import com.zhiyuan.college.model.dto.LoginResponse;
import com.zhiyuan.college.model.dto.ProfileCompletionRequest;
import com.zhiyuan.college.model.dto.RegisterRequest;
import com.zhiyuan.college.service.auth.AuthService;
import jakarta.validation.Valid;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/login")
    public LoginResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @PostMapping("/register")
    public LoginResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/profile")
    public LoginResponse completeProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader,
            @Valid @RequestBody ProfileCompletionRequest request) {
        return authService.completeProfile(extractBearerToken(authHeader), request);
    }

    @GetMapping("/profile")
    public LoginResponse currentProfile(
            @RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        return authService.currentProfile(extractBearerToken(authHeader));
    }

    @PostMapping("/logout")
    public Map<String, String> logout(@RequestHeader(value = HttpHeaders.AUTHORIZATION, required = false) String authHeader) {
        String token = extractBearerToken(authHeader);
        authService.logout(token);
        return Map.of("message", "Logged out");
    }

    private String extractBearerToken(String authHeader) {
        if (authHeader == null || !authHeader.startsWith("Bearer ")) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        String token = authHeader.substring("Bearer ".length()).trim();
        if (token.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing bearer token");
        }
        return token;
    }
}
