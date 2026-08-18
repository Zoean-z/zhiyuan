package com.zhiyuan.college.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.config.CacheRedisProperties;
import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.MetaOptionsResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.Arrays;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class MetaOptionsService {

    private static final Logger log = LoggerFactory.getLogger(MetaOptionsService.class);
    private static final String META_OPTIONS_CACHE_KEY = "meta:options";

    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final CacheRedisProperties cacheRedisProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    public MetaOptionsService(AdmissionCutoffMapper admissionCutoffMapper,
                              CacheRedisProperties cacheRedisProperties,
                              StringRedisTemplate stringRedisTemplate,
                              ObjectMapper objectMapper) {
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.cacheRedisProperties = cacheRedisProperties;
        this.stringRedisTemplate = stringRedisTemplate;
        this.objectMapper = objectMapper;
    }

    public MetaOptionsResponse getOptions() {
        if (cacheRedisProperties.isEnabled()) {
            MetaOptionsResponse cached = readCachedOptions();
            if (cached != null) {
                return cached;
            }
        }

        MetaOptionsResponse response = new MetaOptionsResponse(
                admissionCutoffMapper.findDistinctProvinces(),
                Arrays.stream(SubjectType.values()).map(SubjectType::getDisplayName).toList()
        );
        writeCachedOptions(response);
        return response;
    }

    private MetaOptionsResponse readCachedOptions() {
        try {
            String cachedJson = stringRedisTemplate.opsForValue().get(META_OPTIONS_CACHE_KEY);
            if (cachedJson == null || cachedJson.isBlank()) {
                return null;
            }
            return objectMapper.readValue(cachedJson, MetaOptionsResponse.class);
        } catch (Exception ex) {
            log.warn("Failed to read meta options cache", ex);
            return null;
        }
    }

    private void writeCachedOptions(MetaOptionsResponse response) {
        if (!cacheRedisProperties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    META_OPTIONS_CACHE_KEY,
                    objectMapper.writeValueAsString(response),
                    cacheRedisProperties.getMetaTtl()
            );
        } catch (JsonProcessingException ex) {
            log.warn("Failed to serialize meta options cache payload", ex);
        } catch (Exception ex) {
            log.warn("Failed to write meta options cache", ex);
        }
    }
}
