package com.zhiyuan.college.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.mapper.RecommendationLogMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.HistoryDetailResponse;
import com.zhiyuan.college.model.dto.HistoryRecordResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.entity.RecommendationLog;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class HistoryService {

    public static final String QUERY_TYPE_SCORE = "score";
    public static final String QUERY_TYPE_TEXT = "text";

    private final RecommendationLogMapper recommendationLogMapper;
    private final ObjectMapper objectMapper;

    public HistoryService(RecommendationLogMapper recommendationLogMapper,
                          ObjectMapper objectMapper) {
        this.recommendationLogMapper = recommendationLogMapper;
        this.objectMapper = objectMapper;
    }

    public void saveScoreHistory(Long userId, RecommendationRequest request, Object result) {
        String content = String.format("分数:%s, 省份:%s, 科类:%s",
                safeValue(request.getScore()),
                safeValue(request.getProvince()),
                request.getSubjectType() == null ? "-" : request.getSubjectType().name());
        saveHistory(userId, QUERY_TYPE_SCORE, content, result);
    }

    public void saveTextHistory(Long userId, FreeTextRecommendationRequest request, Object result) {
        String content = request.getRequirementText() == null ? "" : request.getRequirementText().trim();
        if (content.length() > 300) {
            content = content.substring(0, 300) + "...";
        }
        saveHistory(userId, QUERY_TYPE_TEXT, content, result);
    }

    public List<HistoryRecordResponse> listByUser(Long userId) {
        LambdaQueryWrapper<RecommendationLog> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(RecommendationLog::getUserId, userId)
                .orderByDesc(RecommendationLog::getCreatedAt, RecommendationLog::getId);
        return recommendationLogMapper.selectList(wrapper).stream()
                .map(log -> new HistoryRecordResponse(
                        log.getId(),
                        log.getQueryType(),
                        log.getQueryContent(),
                        log.getCreatedAt()))
                .toList();
    }

    public HistoryDetailResponse getById(Long userId, Long id) {
        RecommendationLog log = recommendationLogMapper.selectById(id);
        if (log == null || !userId.equals(log.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "History record not found");
        }
        return new HistoryDetailResponse(
                log.getId(),
                log.getQueryType(),
                log.getQueryContent(),
                log.getResultJson(),
                log.getCreatedAt()
        );
    }

    private void saveHistory(Long userId, String queryType, String queryContent, Object result) {
        RecommendationLog log = new RecommendationLog();
        log.setUserId(userId);
        log.setQueryType(queryType);
        log.setQueryContent(queryContent);
        log.setResultJson(toJson(result));
        recommendationLogMapper.insert(log);
    }

    private String toJson(Object result) {
        try {
            return objectMapper.writeValueAsString(result);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize history result");
        }
    }

    private String safeValue(Object value) {
        return value == null ? "-" : String.valueOf(value);
    }
}
