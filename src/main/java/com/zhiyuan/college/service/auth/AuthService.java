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
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AuthService {

    private static final Logger log = LoggerFactory.getLogger(AuthService.class);

    private static final String BLACKLIST_PREFIX = "auth:blacklist:";

    private final UserAccountMapper userAccountMapper;
    private final JwtTokenService jwtTokenService;
    private final PasswordEncoder passwordEncoder;
    private final StringRedisTemplate stringRedisTemplate;
    private final boolean redisCacheEnabled;
    private final boolean allowLegacyPlaintextLogin;
    private final Map<String, Long> localBlacklist = new ConcurrentHashMap<>();

    public AuthService(UserAccountMapper userAccountMapper,
                       JwtTokenService jwtTokenService,
                       PasswordEncoder passwordEncoder,
                       StringRedisTemplate stringRedisTemplate,
                       @Value("${cache.redis.enabled:false}") boolean redisCacheEnabled,
                       @Value("${auth.allow-legacy-plaintext-login:false}") boolean allowLegacyPlaintextLogin) {
        this.userAccountMapper = userAccountMapper;
        this.jwtTokenService = jwtTokenService;
        this.passwordEncoder = passwordEncoder;
        this.stringRedisTemplate = stringRedisTemplate;
        this.redisCacheEnabled = redisCacheEnabled;
        this.allowLegacyPlaintextLogin = allowLegacyPlaintextLogin;
    }

    public LoginResponse login(LoginRequest request) {
        UserAccount user = userAccountMapper.findByUsername(request.getUsername());
        if (user == null || !Boolean.TRUE.equals(user.getEnabled()) || !passwordMatches(request.getPassword(), user)) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid username or password");
        }
        user.setRole(UserRole.fromValue(userAccountMapper.findRoleByUsername(request.getUsername())));

        // The login form doubles as the "exam profile" form in the SPA, so an explicitly
        // submitted score/subject/province is persisted here on purpose.
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
        return new LoginResponse(token, user.getUsername(), user.getScore(), user.getSubjectType(), user.getExamProvince(), user.getRole());
    }

    public LoginResponse register(RegisterRequest request) {
        String username = normalizeUsername(request.getUsername());
        if (username.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "用户名不能为空");
        }
        if (userAccountMapper.findByUsername(username) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }
        UserAccount user = new UserAccount();
        user.setUsername(username);
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setScore(request.getScore());
        user.setSubjectType(request.getSubjectType());
        user.setExamProvince(isBlank(request.getExamProvince()) ? null : request.getExamProvince().trim());
        user.setRole(UserRole.USER);
        user.setEnabled(true);
        try {
            userAccountMapper.insert(user);
        } catch (DuplicateKeyException ex) {
            // Two concurrent registrations can both pass the check above; the unique index is the
            // authoritative guard, so translate it into the same 409 instead of a 500.
            throw new ResponseStatusException(HttpStatus.CONFLICT, "用户名已存在");
        }

        String token = jwtTokenService.generateToken(user.getId(), user.getUsername(), user.getRole().name());
        return new LoginResponse(token, user.getUsername(), user.getScore(), user.getSubjectType(), user.getExamProvince(), user.getRole());
    }

    public LoginResponse completeProfile(String token, ProfileCompletionRequest request) {
        UserAccount user = validateToken(token);
        if (user == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid or expired token");
        }
        user.setScore(request.getScore());
        user.setSubjectType(request.getSubjectType());
        user.setExamProvince(isBlank(request.getExamProvince()) ? null : request.getExamProvince().trim());
        userAccountMapper.updateById(user);
        return new LoginResponse(
                token,
                user.getUsername(),
                user.getScore(),
                user.getSubjectType(),
                user.getExamProvince(),
                user.getRole()
        );
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

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private String normalizeUsername(String username) {
        return username == null ? "" : username.trim();
    }

    /**
     * Verifies the submitted password against the stored credential.
     *
     * <p>Only BCrypt hashes are accepted by default. Clear-text comparison exists purely for the
     * demo accounts seeded by {@code sql/data.sql} and must be enabled explicitly through
     * {@code auth.allow-legacy-plaintext-login}; when enabled, a successful match immediately
     * rewrites the stored value as a BCrypt hash.
     */
    private boolean passwordMatches(String rawPassword, UserAccount user) {
        String storedPassword = user.getPassword();
        if (storedPassword == null || storedPassword.isBlank()) {
            return false;
        }
        if (isBcryptHash(storedPassword)) {
            return passwordEncoder.matches(rawPassword, storedPassword);
        }
        if (!allowLegacyPlaintextLogin) {
            log.warn("Rejected login for user '{}': the stored password is not hashed and "
                    + "auth.allow-legacy-plaintext-login is disabled.", user.getUsername());
            return false;
        }
        if (!storedPassword.equals(rawPassword)) {
            return false;
        }
        log.warn("Upgrading the clear-text password of user '{}' to BCrypt.", user.getUsername());
        UserAccount update = new UserAccount();
        update.setId(user.getId());
        update.setPassword(passwordEncoder.encode(rawPassword));
        userAccountMapper.updateById(update);
        user.setPassword(update.getPassword());
        return true;
    }

    private boolean isBcryptHash(String storedPassword) {
        return storedPassword.startsWith("$2a$")
                || storedPassword.startsWith("$2b$")
                || storedPassword.startsWith("$2y$");
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
