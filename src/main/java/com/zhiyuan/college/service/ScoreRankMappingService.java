package com.zhiyuan.college.service;

import com.zhiyuan.college.config.CacheRedisProperties;
import com.zhiyuan.college.mapper.ScoreRankMappingMapper;
import com.zhiyuan.college.model.dto.ScoreRankPointResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

/**
 * 分数 -> 位次的解析服务。
 *
 * <p>原版只做精确命中（一分一段表里必须正好有这一分），命不中就返回 null，
 * 推荐会退化成“只比分数”。现在改成三级兑底：</p>
 * <ol>
 *   <li>EXACT：一分一段表精确命中</li>
 *   <li>INTERPOLATED：落在曲线内部，相邻两点线性插值</li>
 *   <li>EXTRAPOLATED：落在曲线两端之外，用端点斜率外推</li>
 * </ol>
 */
@Service
public class ScoreRankMappingService {

    public static final String SOURCE_EXACT = "EXACT";
    public static final String SOURCE_INTERPOLATED = "INTERPOLATED";
    public static final String SOURCE_EXTRAPOLATED = "EXTRAPOLATED";
    public static final String SOURCE_PROVIDED = "PROVIDED";
    public static final String SOURCE_NONE = "NONE";

    private static final Logger log = LoggerFactory.getLogger(ScoreRankMappingService.class);
    private static final String NULL_SENTINEL = "__NULL__";
    private static final Duration CURVE_CACHE_TTL = Duration.ofMinutes(10);

    private final ScoreRankMappingMapper scoreRankMappingMapper;
    private final CacheRedisProperties cacheRedisProperties;
    private final StringRedisTemplate stringRedisTemplate;
    private final ConcurrentHashMap<String, CachedCurve> curveCache = new ConcurrentHashMap<>();

    public ScoreRankMappingService(ScoreRankMappingMapper scoreRankMappingMapper,
                                   CacheRedisProperties cacheRedisProperties,
                                   StringRedisTemplate stringRedisTemplate) {
        this.scoreRankMappingMapper = scoreRankMappingMapper;
        this.cacheRedisProperties = cacheRedisProperties;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    /** 兼容原有调用方：能算出位次就返回（精确 -> 插值 -> 外推）。 */
    public Integer resolveUserRank(String province, String subjectType, Integer score) {
        return resolveRank(province, subjectType, score).rank();
    }

    /** 带来源标记的位次解析。 */
    public RankResolution resolveRank(String province, String subjectType, Integer score) {
        if (province == null || province.isBlank()
                || subjectType == null || subjectType.isBlank()
                || score == null) {
            return RankResolution.none();
        }
        String normalizedProvince = province.trim();
        String normalizedSubject = subjectType.trim();

        Integer exactRank = resolveExactRank(normalizedProvince, normalizedSubject, score);
        if (exactRank != null) {
            return new RankResolution(exactRank, SOURCE_EXACT, mappingYear(normalizedProvince, normalizedSubject));
        }
        return estimateRank(normalizedProvince, normalizedSubject, score);
    }

    /** 考生自己填了位次时以位次为准，否则用一分一段推算。 */
    public RankResolution resolveRankOrProvided(String province, String subjectType, Integer score, Integer providedRank) {
        if (providedRank != null && providedRank > 0) {
            return new RankResolution(providedRank, SOURCE_PROVIDED, mappingYear(province, subjectType));
        }
        return resolveRank(province, subjectType, score);
    }

    /** 最新年份的完整一分一段曲线，按分数升序。 */
    public List<ScoreRankPointResponse> getLatestCurve(String province, String subjectType) {
        if (province == null || province.isBlank() || subjectType == null || subjectType.isBlank()) {
            return List.of();
        }
        return curveEntry(province.trim(), subjectType.trim()).points();
    }

    public Integer getLatestMappingYear(String province, String subjectType) {
        return mappingYear(province, subjectType);
    }

    private Integer mappingYear(String province, String subjectType) {
        if (province == null || province.isBlank() || subjectType == null || subjectType.isBlank()) {
            return null;
        }
        return curveEntry(province.trim(), subjectType.trim()).mappingYear();
    }

    private RankResolution estimateRank(String province, String subjectType, int score) {
        CachedCurve entry = curveEntry(province, subjectType);
        List<ScoreRankPointResponse> curve = entry.points();
        if (curve.isEmpty()) {
            return RankResolution.none();
        }
        if (curve.size() == 1) {
            return new RankResolution(curve.get(0).getRankValue(), SOURCE_EXTRAPOLATED, entry.mappingYear());
        }

        ScoreRankPointResponse below = null;
        ScoreRankPointResponse above = null;
        for (ScoreRankPointResponse point : curve) {
            if (point.getScore() <= score) {
                below = point;
            }
            if (point.getScore() >= score) {
                above = point;
                break;
            }
        }

        if (below != null && above != null) {
            if (below.getScore().intValue() == above.getScore().intValue()) {
                return new RankResolution(below.getRankValue(), SOURCE_EXACT, entry.mappingYear());
            }
            double ratio = (double) (score - below.getScore()) / (double) (above.getScore() - below.getScore());
            double estimated = below.getRankValue() + ratio * (above.getRankValue() - below.getRankValue());
            return new RankResolution(clampRank(estimated), SOURCE_INTERPOLATED, entry.mappingYear());
        }

        if (above == null) {
            ScoreRankPointResponse top = curve.get(curve.size() - 1);
            ScoreRankPointResponse previous = curve.get(curve.size() - 2);
            double estimated = top.getRankValue() + slope(previous, top) * (score - top.getScore());
            return new RankResolution(clampRank(estimated), SOURCE_EXTRAPOLATED, entry.mappingYear());
        }

        ScoreRankPointResponse bottom = curve.get(0);
        ScoreRankPointResponse next = curve.get(1);
        double estimated = bottom.getRankValue() + slope(bottom, next) * (score - bottom.getScore());
        return new RankResolution(clampRank(estimated), SOURCE_EXTRAPOLATED, entry.mappingYear());
    }

    private double slope(ScoreRankPointResponse left, ScoreRankPointResponse right) {
        int scoreSpan = right.getScore() - left.getScore();
        if (scoreSpan == 0) {
            return 0d;
        }
        return (double) (right.getRankValue() - left.getRankValue()) / (double) scoreSpan;
    }

    private int clampRank(double value) {
        long rounded = Math.round(value);
        if (rounded < 1L) {
            return 1;
        }
        if (rounded > Integer.MAX_VALUE) {
            return Integer.MAX_VALUE;
        }
        return (int) rounded;
    }

    private CachedCurve curveEntry(String province, String subjectType) {
        String key = province + ":" + subjectType;
        Instant now = Instant.now();
        CachedCurve cached = curveCache.get(key);
        if (cached != null && cached.expiresAt().isAfter(now)) {
            return cached;
        }

        List<ScoreRankPointResponse> loaded;
        Integer year;
        try {
            loaded = scoreRankMappingMapper.findLatestCurve(province, subjectType);
            year = scoreRankMappingMapper.findLatestMappingYear(province, subjectType);
        } catch (Exception ex) {
            log.warn("Failed to load score-rank curve for {} {}", province, subjectType, ex);
            return cached != null ? cached : CachedCurve.empty(now.plus(CURVE_CACHE_TTL));
        }

        List<ScoreRankPointResponse> sanitized = new ArrayList<>();
        if (loaded != null) {
            for (ScoreRankPointResponse point : loaded) {
                if (point != null && point.getScore() != null && point.getRankValue() != null) {
                    sanitized.add(point);
                }
            }
        }
        CachedCurve entry = new CachedCurve(List.copyOf(sanitized), year, now.plus(CURVE_CACHE_TTL));
        curveCache.put(key, entry);
        return entry;
    }

    private Integer resolveExactRank(String province, String subjectType, Integer score) {
        String cacheKey = buildCacheKey(province, subjectType, score);
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

    /** 位次解析结果：rank 可以为 null（没数据），source 告诉前端这个位次是怎么来的。 */
    public record RankResolution(Integer rank, String source, Integer mappingYear) {

        public static RankResolution none() {
            return new RankResolution(null, SOURCE_NONE, null);
        }

        public boolean present() {
            return rank != null;
        }
    }

    private record CachedCurve(List<ScoreRankPointResponse> points, Integer mappingYear, Instant expiresAt) {

        private static CachedCurve empty(Instant expiresAt) {
            return new CachedCurve(List.of(), null, expiresAt);
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
