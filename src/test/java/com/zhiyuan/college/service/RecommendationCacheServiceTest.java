package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.config.CacheRedisProperties;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

class RecommendationCacheServiceTest {
    @Test
    @SuppressWarnings("unchecked")
    void revisedScoringReadsAndWritesSameNewCacheNamespace() {
        var properties = new CacheRedisProperties(); properties.setEnabled(true);
        var redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        var service = new RecommendationCacheService(properties, redis, new ObjectMapper());
        var request = new RecommendationRequest();
        request.setProvince("湖南"); request.setSubjectType(SubjectType.PHYSICS); request.setScore(650);
        request.setRecommendationMode(RecommendationMode.SCHOOL_FIRST);
        assertNull(service.getRecommendation(request));
        service.cacheRecommendation(request, new RecommendationResponse("request", RecommendationMode.SCHOOL_FIRST,
                3131, List.of(), List.of(), List.of(), "summary", List.of()));
        var readKey = ArgumentCaptor.forClass(String.class);
        var writeKey = ArgumentCaptor.forClass(String.class);
        verify(values).get(readKey.capture());
        verify(values).set(writeKey.capture(), anyString(), eq(properties.getRecommendationTtl()));
        assertEquals(readKey.getValue(), writeKey.getValue());
        assertTrue(readKey.getValue().startsWith("recommendation:relative-rank-v2:"));
        verify(values, never()).get(matches("recommendation:SCHOOL_FIRST:.*"));
    }
}
