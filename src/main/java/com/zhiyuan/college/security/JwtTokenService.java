package com.zhiyuan.college.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.Set;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private static final Logger log = LoggerFactory.getLogger(JwtTokenService.class);

    /** HS256 requires at least 256 bits (32 bytes) of key material. */
    private static final int MINIMUM_KEY_BYTES = 32;

    /** Sample secrets committed in this repository. They must never be used outside local development. */
    private static final Set<String> SAMPLE_SECRETS = Set.of(
            "emhpeXVhbi1kZXYtc2VjcmV0LXpoaXl1YW4tZGV2LXNlY3JldC0yMDI2LWtleQ==",
            "emhpeXVhbi1wcm9kLXNlY3JldC16aGl5dWFuLXByb2Qtc2VjcmV0LTIwMjYta2V5",
            "zhiyuan-dev-secret-zhiyuan-dev-secret-2026"
    );

    private final SecretKey signingKey;
    private final long tokenTtlSeconds;
    private final String issuer;

    public JwtTokenService(@Value("${auth.jwt-secret}") String jwtSecret,
                           @Value("${auth.token-ttl-seconds:86400}") long tokenTtlSeconds,
                           @Value("${auth.jwt-issuer:zhiyuan}") String issuer) {
        this.signingKey = buildKey(jwtSecret);
        this.tokenTtlSeconds = tokenTtlSeconds;
        this.issuer = issuer;
    }

    public String generateToken(Long userId, String username, String role) {
        Instant now = Instant.now();
        Instant expireAt = now.plusSeconds(tokenTtlSeconds);
        return Jwts.builder()
                .id(UUID.randomUUID().toString())
                .issuer(issuer)
                .subject(String.valueOf(userId))
                .claim("username", username)
                .claim("role", role)
                .issuedAt(Date.from(now))
                .expiration(Date.from(expireAt))
                .signWith(signingKey)
                .compact();
    }

    public Claims parseClaims(String token) {
        return Jwts.parser()
                .verifyWith(signingKey)
                .build()
                .parseSignedClaims(token)
                .getPayload();
    }

    public long remainingSeconds(String token) {
        Date expiration = parseClaims(token).getExpiration();
        long seconds = expiration.toInstant().getEpochSecond() - Instant.now().getEpochSecond();
        return Math.max(seconds, 0);
    }

    public String extractJti(String token) {
        return parseClaims(token).getId();
    }

    /**
     * Builds the HMAC signing key from the configured secret.
     *
     * <p>The secret is never padded or otherwise "repaired": a short secret means the deployment is
     * insecure, so startup fails fast instead of silently signing tokens with predictable key
     * material.
     */
    private SecretKey buildKey(String jwtSecret) {
        if (jwtSecret == null || jwtSecret.isBlank()) {
            throw new IllegalStateException(
                    "auth.jwt-secret (AUTH_JWT_SECRET) is required. Generate one with `openssl rand -base64 48`.");
        }
        String trimmedSecret = jwtSecret.trim();
        if (SAMPLE_SECRETS.contains(trimmedSecret)) {
            log.warn("auth.jwt-secret is still one of the sample values committed in this repository. "
                    + "Set AUTH_JWT_SECRET to a private value before exposing this service.");
        }
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(trimmedSecret);
        } catch (RuntimeException ex) {
            bytes = trimmedSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < MINIMUM_KEY_BYTES) {
            throw new IllegalStateException(("auth.jwt-secret must provide at least %d bytes of key material, "
                    + "but the configured value only provides %d. Generate one with `openssl rand -base64 48`.")
                    .formatted(MINIMUM_KEY_BYTES, bytes.length));
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
