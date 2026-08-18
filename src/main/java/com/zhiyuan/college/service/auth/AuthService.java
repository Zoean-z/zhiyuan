package com.zhiyuan.college.service.auth;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.LoginRequest;
import com.zhiyuan.college.model.dto.LoginResponse;
import com.zhiyuan.college.model.dto.ProfileCompletionRequest;
import com.zhiyuan.college.model.dto.RegisterRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.UserRole;
import com.zhiyuan.college.security.JwtTokenService;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import java.time.Duration;
import java.util.Map;
import java.util.HashSet;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisCacheEnabled;
    private final Map<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public AuthService(UserAccountMapper userAccountMapper,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder,
                       StringRedisTemplate stringRedisTemplate,
                       @Value("${cache.redis.enabled:false}") boolean redisCacheEnabled) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisCacheEnabled = redisCacheEnabled;
    }

    public LoginResponse login(LoginRequest request) {
        return authenticate(request, false);
    }

    public LoginResponse adminLogin(LoginRequest request) {
        return authenticate(request, true);
    }

    private LoginResponse authenticate(LoginRequest request, boolean adminOnly) {
        UserAccount user = userAccountMapper.findByUsername(request.getUsername());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !passwordMatches(request.getPassword(), user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        user.setRole(UserRole.fromValue(userAccountMapper.findRoleByUsername(request.getUsername())));
        if (adminOnly && user.getRole() != UserRole.ADMIN) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid administrator credentials");
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

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return loginResponse(token, user);
    }

    public LoginResponse register(RegisterRequest request) {
        if (!request.isSliderVerified()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请完成滑块验证");
        }
        String username = normalizeUsername(request.getUsername());
        if (userAccountMapper.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setEmail(null);
        user.setScore(request.getScore());
        user.setSubjectType(request.getSubjectType());
        user.setExamProvince(isBlank(request.getExamProvince()) ? null : request.getExamProvince().trim());
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        userAccountMapper.insert(user);

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return loginResponse(token, user);
    }

    public LoginResponse completeProfile(String token, ProfileCompletionRequest request) {
        UserAccount user = validateToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        user.setScore(request.getScore());
        user.setSubjectType(request.getSubjectType());
        user.setExamProvince(request.getExamProvince().trim());
        if (new HashSet<>(request.getElectiveSubjects()).size() != 2) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "electiveSubjects must contain two distinct subjects");
        }
        user.setElectiveSubjects(request.getElectiveSubjects());
        userAccountMapper.updateById(user);
        return loginResponse(token, user);
    }

    public UserAccount validateToken(String token) {
        if (isBlacklisted(token)) {
            return null;
        }
        try {
            Claims claims = jwtTokenService.parseClaims(token);
            Long userId = Long.valueOf(claims.getSubject());
            UserAccount user = userAccountMapper.findByIdCompat(userId);
            if (user != null && !Boolean.TRUE.equals(user.getEnabled())) {
                return null;
            }
            return user;
        } catch (JwtException | IllegalArgumentException ex) {
            return null;
        }
    }

    public void logout(String token) {
        try {
            long ttlSeconds = jwtTokenService.remainingSeconds(token);
            if (ttlSeconds <= 0) {
                return;
            }
            blacklistToken(jwtTokenService.extractJti(token), ttlSeconds);
        } catch (JwtException ex) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
    }

    public void updateScore(Long userId, Integer score) {
        UserAccount user = new UserAccount();
        user.setId(userId);
        user.setScore(score);
        userAccountMapper.updateById(user);
    }

    private LoginResponse loginResponse(String token, UserAccount user) {
        return new LoginResponse(token, user.getUsername(), user.getScore(), user.getSubjectType(),
                user.getExamProvince(), user.getElectiveSubjects(), user.getRole());
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    private boolean passwordMatches(String rawPassword, UserAccount user) {
        String storedPassword = user.getPassword();
        if (storedPassword == null) {
            return false;
        }
        if (storedPassword.startsWith("$2a$") || storedPassword.startsWith("$2b$") || storedPassword.startsWith("$2y$")) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (!storedPassword.equals(rawPassword)) {
            return false;
        }
        UserAccount update = new UserAccount();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(rawPassword));
        userAccountMapper.updateById(update);
        user.setPassword(update.getPassword());
        return true;
    }

    private boolean isBlacklisted(String token) {
        cleanupLocalBlacklist();
        String jti = jwtTokenService.extractJti(token);
        if (localBlacklist.containsKey(jti)) {
            return true;
        }
        if (!redisCacheEnabled) {
            return false;
        }
        try {
            return Boolean.TRUE.equals(stringRedisTemplate.hasKey(BLACKLIST_PREFIX + jti));
        } catch (Exception ex) {
            return false;
        }
    }

    private void blacklistToken(String jti, long ttlSeconds) {
        localBlacklist.put(jti, System.currentTimeMillis() + ttlSeconds * 1000);
        if (!redisCacheEnabled) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(BLACKLIST_PREFIX + jti, "1", Duration.ofSeconds(ttlSeconds));
        } catch (Exception ignore) {
        }
    }

    private void cleanupLocalBlacklist() {
        long now = System.currentTimeMillis();
        localBlacklist.entrySet().removeIf(entry -> entry.getValue() <= now);
    }
}
