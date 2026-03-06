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

        if (request.getScore() != null && !request.getScore().equals(user.getScore())) {
            user.setScore(request.getScore());
            userAccountMapper.updateById(user);
        }

        String token = UUID.randomUUID().toString().replace("-", "");
        long expireAtEpochSecond = Instant.now().getEpochSecond() + tokenTtlSeconds;
        sessions.put(token, new SessionInfo(user.getId(), expireAtEpochSecond));
        return new LoginResponse(token, user.getUsername(), user.getScore());
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
        return userAccountMapper.selectById(sessionInfo.userId());
    }

    public void updateScore(Long userId, Integer score) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setScore(score);
        userAccountMapper.updateById(user);
    }

    private record SessionInfo(Long userId, long expireAtEpochSecond) {}
}

