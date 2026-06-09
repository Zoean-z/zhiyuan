package com.zhiyuan.college.service;

import com.zhiyuan.college.config.CacheRedisProperties;
import com.zhiyuan.college.mapper.ScoreRankMappingMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

@Service
public class ScoreRankMappingService {

    private static final Logger log = LoggerFactory.getLogger(ScoreRankMappingService.class);
    private static final String NULL_SENTINEL = "__NULL__";

    private final ScoreRankMappingMapper scoreRankMappingMapper;
    private final CacheRedisProperties cacheRedisProperties;
    private final StringRedisTemplate stringRedisTemplate;

    public ScoreRankMappingService(ScoreRankMappingMapper scoreRankMappingMapper,
                                   CacheRedisProperties cacheRedisProperties,
                                   StringRedisTemplate stringRedisTemplate) {
        this.scoreRankMappingMapper = scoreRankMappingMapper;
        this.cacheRedisProperties = cacheRedisProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    public Integer resolveUserRank(String province, String subjectType, Integer score) {
        if (province == null || province.isBlank() || subjectType == null || score == null) {
            return null;
        }

        String cacheKey = buildCacheKey(province.trim(), subjectType, score);
        if (cacheRedisProperties.isEnabled()) {
            CacheLookup cacheLookup = readCachedRank(cacheKey);
            if (cacheLookup.hit()) {
                return cacheLookup.rank();
            }
        }

        Integer resolvedRank = scoreRankMappingMapper.findLatestRankValueByProvinceSubjectAndScore(province, subjectType, score);
        writeCachedRank(cacheKey, resolvedRank);
        return resolvedRank;
    }

    private String buildCacheKey(String province, String subjectType, Integer score) {
        return "score-rank:%s:%s:%d".formatted(province, subjectType, score);
    }

    private CacheLookup readCachedRank(String cacheKey) {
        try {
            String cachedValue = stringRedisTemplate.opsForValue().get(cacheKey);
            if (cachedValue == null) {
                return CacheLookup.miss();
            }
            if (NULL_SENTINEL.equals(cachedValue)) {
                return CacheLookup.hit(null);
            }
            return CacheLookup.hit(Integer.valueOf(cachedValue));
        } catch (Exception ex) {
            log.warn("Failed to read score-rank cache for key {}", cacheKey, ex);
            return CacheLookup.miss();
        }
    }

    private void writeCachedRank(String cacheKey, Integer resolvedRank) {
        if (!cacheRedisProperties.isEnabled()) {
            return;
        }
        try {
            stringRedisTemplate.opsForValue().set(
                    cacheKey,
                    resolvedRank == null ? NULL_SENTINEL : resolvedRank.toString(),
                    cacheRedisProperties.getMetaTtl()
            );
        } catch (Exception ex) {
            log.warn("Failed to write score-rank cache for key {}", cacheKey, ex);
        }
    }

    private record CacheLookup(boolean hit, Integer rank) {

        private static CacheLookup hit(Integer rank) {
            return new CacheLookup(true, rank);
        }

        private static CacheLookup miss() {
            return new CacheLookup(false, null);
        }
    }
}
