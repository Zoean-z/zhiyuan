package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.config.CacheRedisProperties;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class RecommendationCacheService {

    private static final Logger log = LoggerFactory.getLogger(RecommendationCacheService.class);

    private final CacheRedisProperties cacheRedisProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public RecommendationCacheService(CacheRedisProperties cacheRedisProperties,
                                      StringRedisTemplate stringRedisTemplate,
                                      ObjectMapper objectMapper) {
        this.cacheRedisProperties = cacheRedisProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public RecommendationResponse getRecommendation(RecommendationRequest request) {
        RecommendationResponseSnapshot snapshot = readValue(recommendationCacheKey(request), RecommendationResponseSnapshot.class);
        return snapshot == null ? null : snapshot.toResponse();
    }

    public void cacheRecommendation(RecommendationRequest request, RecommendationResponse response) {
        writeValue(recommendationCacheKey(request), RecommendationResponseSnapshot.from(response));
    }

    public AiRequirementParserService.ParseResult getParsedRequirement(String text) {
        ParsedRequirementSnapshot snapshot = readValue(parseCacheKey(text), ParsedRequirementSnapshot.class);
        return snapshot == null ? null : snapshot.toParseResult();
    }

    public void cacheParsedRequirement(String text, AiRequirementParserService.ParseResult parseResult) {
        writeValue(parseCacheKey(text), ParsedRequirementSnapshot.from(parseResult));
    }

    private <T> T readValue(String key, Class<T> clazz) {
        if (!cacheRedisProperties.isEnabled()) {
            return null;
        }
        try {
            String value = stringRedisTemplate.opsForValue().get(key);
            if (value == null || value.isBlank()) {
                return null;
            }
            return objectMapper.readValue(value, clazz);
        } catch (Exception ex) {
            log.warn("Failed to read cache key {}", key, ex);
            return null;
        }
    }

    private void writeValue(String key, Object value) {
        if (!cacheRedisProperties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    key,
                    objectMapper.writeValueAsString(value),
                    cacheRedisProperties.getRecommendationTtl()
            );
        } catch (Exception ex) {
            log.warn("Failed to write cache key {}", key, ex);
        }
    }

    private String recommendationCacheKey(RecommendationRequest request) {
        String mode = request.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST.name()
                : request.getRecommendationMode().name();
        String province = request.getProvince() == null ? "" : request.getProvince().trim();
        String subjectType = request.getSubjectType() == null ? "" : request.getSubjectType().name();
        String majorKeyword = request.getMajorKeyword() == null ? "" : request.getMajorKeyword().trim().toLowerCase();
        Integer score = request.getScore() == null ? -1 : request.getScore();
        return "recommendation:%s:%s:%s:%d:%s".formatted(mode, province, subjectType, score, digest(majorKeyword));
    }

    private String parseCacheKey(String text) {
        String normalized = text == null ? "" : text.trim().replaceAll("\\s+", " ");
        return "ai-parse:%s".formatted(digest(normalized));
    }

    private String digest(String source) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hashed = digest.digest(source.getBytes(StandardCharsets.UTF_8));
            StringBuilder builder = new StringBuilder(hashed.length * 2);
            for (byte value : hashed) {
                builder.append(String.format("%02x", value));
            }
            return builder.toString();
        } catch (NoSuchAlgorithmException ex) {
            throw new IllegalStateException("SHA-256 is not available", ex);
        }
    }

    public static class RecommendationResponseSnapshot {

        private RecommendationMode recommendationMode;
        private Integer userRank;
        private List<RecommendationItemResponse> rush;
        private List<RecommendationItemResponse> safe;
        private List<RecommendationItemResponse> guarantee;
        private String summary;
        private List<String> tips;

        public static RecommendationResponseSnapshot from(RecommendationResponse response) {
            RecommendationResponseSnapshot snapshot = new RecommendationResponseSnapshot();
            snapshot.setRecommendationMode(response.getRecommendationMode());
            snapshot.setUserRank(response.getUserRank());
            snapshot.setRush(response.getRush());
            snapshot.setSafe(response.getSafe());
            snapshot.setGuarantee(response.getGuarantee());
            snapshot.setSummary(response.getSummary());
            snapshot.setTips(response.getTips());
            return snapshot;
        }

        public RecommendationResponse toResponse() {
            return new RecommendationResponse(
                    UUID.randomUUID().toString(),
                    recommendationMode,
                    userRank,
                    rush,
                    safe,
                    guarantee,
                    summary,
                    tips
            );
        }

        public RecommendationMode getRecommendationMode() {
            return recommendationMode;
        }

        public void setRecommendationMode(RecommendationMode recommendationMode) {
            this.recommendationMode = recommendationMode;
        }

        public Integer getUserRank() {
            return userRank;
        }

        public void setUserRank(Integer userRank) {
            this.userRank = userRank;
        }

        public List<RecommendationItemResponse> getRush() {
            return rush;
        }

        public void setRush(List<RecommendationItemResponse> rush) {
            this.rush = rush;
        }

        public List<RecommendationItemResponse> getSafe() {
            return safe;
        }

        public void setSafe(List<RecommendationItemResponse> safe) {
            this.safe = safe;
        }

        public List<RecommendationItemResponse> getGuarantee() {
            return guarantee;
        }

        public void setGuarantee(List<RecommendationItemResponse> guarantee) {
            this.guarantee = guarantee;
        }

        public String getSummary() {
            return summary;
        }

        public void setSummary(String summary) {
            this.summary = summary;
        }

        public List<String> getTips() {
            return tips;
        }

        public void setTips(List<String> tips) {
            this.tips = tips;
        }
    }

    public static class ParsedRequirementSnapshot {

        private ParsedRequirement parsedRequirement;
        private String provider;
        private String modelName;
        private String parseMode;
        private Boolean successFlag;
        private String rawResponse;
        private String errorMessage;

        public static ParsedRequirementSnapshot from(AiRequirementParserService.ParseResult parseResult) {
            ParsedRequirementSnapshot snapshot = new ParsedRequirementSnapshot();
            snapshot.setParsedRequirement(parseResult.parsedRequirement());
            snapshot.setProvider(parseResult.parseTrace().provider());
            snapshot.setModelName(parseResult.parseTrace().modelName());
            snapshot.setParseMode(parseResult.parseTrace().parseMode());
            snapshot.setSuccessFlag(parseResult.parseTrace().successFlag());
            snapshot.setRawResponse(parseResult.parseTrace().rawResponse());
            snapshot.setErrorMessage(parseResult.parseTrace().errorMessage());
            return snapshot;
        }

        public AiRequirementParserService.ParseResult toParseResult() {
            return new AiRequirementParserService.ParseResult(
                    parsedRequirement,
                    new AiRequirementParserService.ParseTrace(
                            provider,
                            modelName,
                            "CACHE_HIT",
                            successFlag,
                            rawResponse,
                            errorMessage
                    )
            );
        }

        public ParsedRequirement getParsedRequirement() {
            return parsedRequirement;
        }

        public void setParsedRequirement(ParsedRequirement parsedRequirement) {
            this.parsedRequirement = parsedRequirement;
        }

        public String getProvider() {
            return provider;
        }

        public void setProvider(String provider) {
            this.provider = provider;
        }

        public String getModelName() {
            return modelName;
        }

        public void setModelName(String modelName) {
            this.modelName = modelName;
        }

        public String getParseMode() {
            return parseMode;
        }

        public void setParseMode(String parseMode) {
            this.parseMode = parseMode;
        }

        public Boolean getSuccessFlag() {
            return successFlag;
        }

        public void setSuccessFlag(Boolean successFlag) {
            this.successFlag = successFlag;
        }

        public String getRawResponse() {
            return rawResponse;
        }

        public void setRawResponse(String rawResponse) {
            this.rawResponse = rawResponse;
        }

        public String getErrorMessage() {
            return errorMessage;
        }

        public void setErrorMessage(String errorMessage) {
            this.errorMessage = errorMessage;
        }
    }
}
