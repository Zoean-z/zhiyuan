package com.zhiyuan.college.security;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.io.Decoders;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.Date;
import java.util.UUID;
import javax.crypto.SecretKey;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class JwtTokenService {

    private final SecretKey signingKey;
    private final long tokenTtlSeconds;
    private final String issuer;

    public JwtTokenService(@Value("${auth.jwt-secret:zhiyuan-dev-secret-zhiyuan-dev-secret-2026}") String jwtSecret,
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

    private SecretKey buildKey(String jwtSecret) {
        byte[] bytes;
        try {
            bytes = Decoders.BASE64.decode(jwtSecret);
        } catch (IllegalArgumentException ex) {
            bytes = jwtSecret.getBytes(StandardCharsets.UTF_8);
        }
        if (bytes.length < 32) {
            byte[] padded = new byte[32];
            System.arraycopy(bytes, 0, padded, 0, Math.min(bytes.length, padded.length));
            for (int index = bytes.length; index < padded.length; index++) {
                padded[index] = (byte) ('a' + (index % 26));
            }
            bytes = padded;
        }
        return Keys.hmacShaKeyFor(bytes);
    }
}
