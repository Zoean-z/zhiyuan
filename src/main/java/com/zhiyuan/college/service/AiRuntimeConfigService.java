package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.AiRuntimeConfigRequest;
import com.zhiyuan.college.model.dto.AiRuntimeConfigResponse;
import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.util.Base64;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AiRuntimeConfigService {

    private static final int CONFIG_ID = 1;
    private static final byte[] AAD = "zhiyuan-ai-runtime-config-v1".getBytes(StandardCharsets.UTF_8);

    private final JdbcTemplate jdbcTemplate;
    private final String defaultBaseUrl;
    private final String defaultApiKey;
    private final String defaultModel;
    private final SecretKeySpec encryptionKey;
    private final SecureRandom secureRandom = new SecureRandom();

    public AiRuntimeConfigService(
            JdbcTemplate jdbcTemplate,
            @Value("${ai.qwen.base-url}") String defaultBaseUrl,
            @Value("${ai.qwen.api-key:}") String defaultApiKey,
            @Value("${ai.qwen.model}") String defaultModel,
            @Value("${auth.jwt-secret}") String jwtSecret) {
        this.jdbcTemplate = jdbcTemplate;
        this.defaultBaseUrl = normalizeBaseUrl(defaultBaseUrl);
        this.defaultApiKey = defaultApiKey == null ? "" : defaultApiKey.trim();
        this.defaultModel = defaultModel.trim();
        this.encryptionKey = deriveKey(jwtSecret);
    }

    public ResolvedAiConfig resolve() {
        StoredAiConfig stored = loadStored();
        if (stored == null) {
            return new ResolvedAiConfig("openai-compatible", defaultBaseUrl, defaultModel, defaultApiKey);
        }
        String storedApiKey = decryptNullable(stored.encryptedApiKey());
        String effectiveApiKey = storedApiKey == null ? defaultApiKey : storedApiKey;
        return new ResolvedAiConfig(stored.provider(), stored.baseUrl(), stored.model(), effectiveApiKey);
    }

    public ResolvedAiConfig resolveForTest(AiRuntimeConfigRequest request) {
        String provider = request.getProvider().trim();
        String baseUrl = normalizeAndValidateBaseUrl(request.getBaseUrl());
        String model = request.getModel().trim();
        String requestApiKey = request.getApiKey();
        String apiKey = requestApiKey == null || requestApiKey.isBlank()
                ? resolve().apiKey()
                : requestApiKey.trim();
        return new ResolvedAiConfig(provider, baseUrl, model, apiKey);
    }

    public AiRuntimeConfigResponse getForAdmin() {
        StoredAiConfig stored = loadStored();
        ResolvedAiConfig resolved = resolve();
        boolean databaseKey = stored != null && stored.encryptedApiKey() != null && !stored.encryptedApiKey().isBlank();
        String source = databaseKey ? "database" : (defaultApiKey.isBlank() ? "none" : "environment");
        return response(resolved, source);
    }

    public synchronized AiRuntimeConfigResponse update(AiRuntimeConfigRequest request) {
        String provider = request.getProvider().trim();
        String baseUrl = normalizeAndValidateBaseUrl(request.getBaseUrl());
        String model = request.getModel().trim();
        StoredAiConfig existing = loadStored();
        String encryptedApiKey = existing == null ? null : existing.encryptedApiKey();
        if (request.isClearApiKey()) {
            encryptedApiKey = null;
        } else if (request.getApiKey() != null && !request.getApiKey().isBlank()) {
            encryptedApiKey = encrypt(request.getApiKey().trim());
        }

        int updated = jdbcTemplate.update("""
                UPDATE ai_runtime_config
                SET provider = ?, base_url = ?, model = ?, encrypted_api_key = ?, updated_at = CURRENT_TIMESTAMP
                WHERE id = ?
                """, provider, baseUrl, model, encryptedApiKey, CONFIG_ID);
        if (updated == 0) {
            jdbcTemplate.update("""
                    INSERT INTO ai_runtime_config (id, provider, base_url, model, encrypted_api_key, updated_at)
                    VALUES (?, ?, ?, ?, ?, CURRENT_TIMESTAMP)
                    """, CONFIG_ID, provider, baseUrl, model, encryptedApiKey);
        }
        return getForAdmin();
    }

    private AiRuntimeConfigResponse response(ResolvedAiConfig config, String source) {
        boolean configured = config.apiKey() != null && !config.apiKey().isBlank();
        String masked = configured ? "••••" + config.apiKey().substring(Math.max(0, config.apiKey().length() - 4)) : "";
        return new AiRuntimeConfigResponse(
                config.provider(), config.baseUrl(), config.model(), configured, masked, source);
    }

    private StoredAiConfig loadStored() {
        return jdbcTemplate.query("""
                        SELECT provider, base_url, model, encrypted_api_key
                        FROM ai_runtime_config WHERE id = ?
                        """,
                (resultSet, rowNumber) -> new StoredAiConfig(
                        resultSet.getString("provider"),
                        resultSet.getString("base_url"),
                        resultSet.getString("model"),
                        resultSet.getString("encrypted_api_key")),
                CONFIG_ID).stream().findFirst().orElse(null);
    }

    private String normalizeAndValidateBaseUrl(String value) {
        String normalized = normalizeBaseUrl(value);
        try {
            URI uri = URI.create(normalized);
            if (!("http".equalsIgnoreCase(uri.getScheme()) || "https".equalsIgnoreCase(uri.getScheme()))
                    || uri.getHost() == null) {
                throw new IllegalArgumentException("invalid URL");
            }
            return normalized;
        } catch (IllegalArgumentException ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "AI 接口地址必须是有效的 HTTP(S) 地址");
        }
    }

    private String normalizeBaseUrl(String value) {
        String normalized = value == null ? "" : value.trim();
        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }
        return normalized;
    }

    private SecretKeySpec deriveKey(String secret) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(secret.getBytes(StandardCharsets.UTF_8));
            return new SecretKeySpec(digest, "AES");
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot initialize AI config encryption", ex);
        }
    }

    private String encrypt(String plainText) {
        try {
            byte[] iv = new byte[12];
            secureRandom.nextBytes(iv);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.ENCRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            cipher.updateAAD(AAD);
            byte[] encrypted = cipher.doFinal(plainText.getBytes(StandardCharsets.UTF_8));
            return "v1:" + Base64.getEncoder().encodeToString(iv) + ":"
                    + Base64.getEncoder().encodeToString(encrypted);
        } catch (GeneralSecurityException ex) {
            throw new IllegalStateException("Cannot encrypt AI config", ex);
        }
    }

    private String decryptNullable(String encrypted) {
        if (encrypted == null || encrypted.isBlank()) {
            return null;
        }
        try {
            String[] parts = encrypted.split(":", 3);
            if (parts.length != 3 || !"v1".equals(parts[0])) {
                throw new GeneralSecurityException("Unsupported encrypted value");
            }
            byte[] iv = Base64.getDecoder().decode(parts[1]);
            byte[] cipherText = Base64.getDecoder().decode(parts[2]);
            Cipher cipher = Cipher.getInstance("AES/GCM/NoPadding");
            cipher.init(Cipher.DECRYPT_MODE, encryptionKey, new GCMParameterSpec(128, iv));
            cipher.updateAAD(AAD);
            return new String(cipher.doFinal(cipherText), StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException ex) {
            throw new IllegalStateException("Stored AI API key cannot be decrypted", ex);
        }
    }

    private record StoredAiConfig(String provider, String baseUrl, String model, String encryptedApiKey) {
    }

    public record ResolvedAiConfig(String provider, String baseUrl, String model, String apiKey) {
    }
}
