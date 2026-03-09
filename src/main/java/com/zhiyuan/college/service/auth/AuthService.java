package com.zhiyuan.college.service.auth;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.LoginRequest;
import com.zhiyuan.college.model.dto.LoginResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private final UserAccountMapper userAccountMapper;
    private final long tokenTtlSeconds;
    private final Map<String, SessionInfo> sessions = new ConcurrentHashMap<>();

    public AuthService(UserAccountMapper userAccountMapper,
                       @Value("${auth.token-ttl-seconds:86400}") long tokenTtlSeconds) {
        this.userAccountMapper = userAccountMapper;
        this.tokenTtlSeconds = tokenTtlSeconds;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountMapper.findByUsername(request.getUsername());
        if (user == null || !user.getPassword().equals(request.getPassword())) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }

        if (user.getScore() == null && request.getScore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is required at first login");
        }
        if (user.getSubjectType() == null && request.getSubjectType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Subject type is required at first login");
        }
        if (isBlank(user.getExamProvince()) && isBlank(request.getExamProvince())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Exam province is required at first login");
        }

        boolean profileChanged = false;
        if (request.getScore() != null && !request.getScore().equals(user.getScore())) {
            user.setScore(request.getScore());
            profileChanged = true;
        }
        if (request.getSubjectType() != null && request.getSubjectType() != user.getSubjectType()) {
            user.setSubjectType(request.getSubjectType());
            profileChanged = true;
        }
        if (!isBlank(request.getExamProvince()) && !request.getExamProvince().equals(user.getExamProvince())) {
            user.setExamProvince(request.getExamProvince().trim());
            profileChanged = true;
        }
        if (profileChanged) {
            userAccountMapper.updateById(user);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAtEpochSecond = Instant.now().getEpochSecond() + tokenTtlSeconds;
        sessions.put(token, new SessionInfo(user.getId(), expireAtEpochSecond));
        return new LoginResponse(token, user.getUsername(), user.getScore(), user.getSubjectType(), user.getExamProvince());
    }

    public UserAccount validateToken(String token) {
        SessionInfo sessionInfo = sessions.get(token);
        if (sessionInfo == null) {
            return null;
        }
        if (sessionInfo.expireAtEpochSecond() < Instant.now().getEpochSecond()) {
            sessions.remove(token);
            return null;
        }
        return userAccountMapper.findByIdCompat(sessionInfo.userId());
    }

    public void logout(String token) {
        sessions.remove(token);
    }

    public void updateScore(Long userId, Integer score) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setScore(score);
        userAccountMapper.updateById(user);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private record SessionInfo(Long userId, long expireAtEpochSecond) {}
}
