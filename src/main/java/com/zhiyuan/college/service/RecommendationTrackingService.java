package com.zhiyuan.college.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyuan.college.mapper.AiParseLogMapper;
import com.zhiyuan.college.mapper.RecommendationTaskMapper;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskResponse;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.entity.AiParseLog;
import com.zhiyuan.college.model.entity.RecommendationTask;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class RecommendationTrackingService {

    private static final String SOURCE_SCORE = "score";
    private static final String SOURCE_TEXT = "text";
    private static final String STATUS_SUCCESS = "SUCCESS";
    private static final String STATUS_PENDING = "PENDING";
    private static final String STATUS_RUNNING = "RUNNING";
    private static final String STATUS_FAILED = "FAILED";

    private final RecommendationTaskMapper recommendationTaskMapper;
    private final AiParseLogMapper aiParseLogMapper;
    private final ObjectMapper objectMapper;

    public RecommendationTrackingService(RecommendationTaskMapper recommendationTaskMapper,
                                         AiParseLogMapper aiParseLogMapper,
                                         ObjectMapper objectMapper) {
        this.recommendationTaskMapper = recommendationTaskMapper;
        this.aiParseLogMapper = aiParseLogMapper;
        this.objectMapper = objectMapper;
    }

    public void saveScoreTask(Long userId,
                              String requestId,
                              RecommendationRequest request,
                              RecommendationResponse response) {
        RecommendationTask task = new RecommendationTask();
        task.setUserId(userId);
        task.setRequestId(requestId);
        task.setSourceType(SOURCE_SCORE);
        task.setRawQuery(buildScoreRawQuery(request));
        task.setRequestJson(toJson(request));
        task.setResultJson(toJson(response));
        task.setStatus(STATUS_SUCCESS);
        task.setRecommendationMode(response.getRecommendationMode() == null ? null : response.getRecommendationMode().name());
        task.setResultCount(countRecommendationItems(response));
        task.setDurationMs(0L);
        recommendationTaskMapper.insert(task);
    }

    public RecommendationTask createPendingTextTask(Long userId,
                                                    String requestId,
                                                    FreeTextRecommendationRequest request) {
        RecommendationTask task = new RecommendationTask();
        task.setUserId(userId);
        task.setRequestId(requestId);
        task.setSourceType(SOURCE_TEXT);
        task.setRawQuery(request.getRequirementText());
        task.setRequestJson(toJson(request));
        task.setStatus(STATUS_PENDING);
        task.setResultCount(0);
        task.setDurationMs(null);
        recommendationTaskMapper.insert(task);
        return task;
    }

    public void markTaskRunning(Long taskId) {
        RecommendationTask update = new RecommendationTask();
        update.setId(taskId);
        update.setStatus(STATUS_RUNNING);
        update.setErrorMessage(null);
        recommendationTaskMapper.updateById(update);
    }

    public void markTextTaskSuccess(Long taskId,
                                    ParsedRequirement parsed,
                                    FreeTextRecommendationResponse response,
                                    AiRequirementParserService.ParseTrace parseTrace,
                                    long durationMs) {
        RecommendationTask existing = requiredTask(taskId);
        RecommendationTask update = new RecommendationTask();
        update.setId(taskId);
        update.setParsedRequirementJson(toJson(parsed));
        update.setResultJson(toJson(response));
        update.setStatus(STATUS_SUCCESS);
        update.setRecommendationMode(parsed.getRecommendationMode() == null ? null : parsed.getRecommendationMode().name());
        update.setResultCount(response.getRecommendations() == null ? 0 : response.getRecommendations().size());
        update.setDurationMs(durationMs);
        update.setErrorMessage(null);
        recommendationTaskMapper.updateById(update);

        saveAiParseLog(taskId, existing.getRequestId(), existing.getRawQuery(), parsed, parseTrace);
    }

    public void markTaskFailed(Long taskId, String errorMessage, long durationMs) {
        RecommendationTask update = new RecommendationTask();
        update.setId(taskId);
        update.setStatus(STATUS_FAILED);
        update.setErrorMessage(errorMessage);
        update.setDurationMs(durationMs);
        recommendationTaskMapper.updateById(update);
    }

    public FreeTextRecommendationTaskResponse getTextTask(Long userId, Long taskId) {
        RecommendationTask task = requiredTask(taskId);
        if (task.getUserId() == null || !task.getUserId().equals(userId)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation task not found");
        }
        if (!SOURCE_TEXT.equals(task.getSourceType())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Task is not a free-text recommendation task");
        }
        return new FreeTextRecommendationTaskResponse(
                task.getId(),
                task.getRequestId(),
                task.getStatus(),
                task.getSourceType(),
                task.getRecommendationMode(),
                task.getResultCount(),
                task.getDurationMs(),
                task.getErrorMessage(),
                task.getCreatedAt(),
                task.getUpdatedAt(),
                fromJson(task.getParsedRequirementJson(), ParsedRequirement.class),
                fromJson(task.getResultJson(), FreeTextRecommendationResponse.class)
        );
    }

    public void saveTextTask(Long userId,
                             String requestId,
                             FreeTextRecommendationRequest request,
                             ParsedRequirement parsed,
                             FreeTextRecommendationResponse response,
                             AiRequirementParserService.ParseTrace parseTrace) {
        RecommendationTask task = new RecommendationTask();
        task.setUserId(userId);
        task.setRequestId(requestId);
        task.setSourceType(SOURCE_TEXT);
        task.setRawQuery(request.getRequirementText());
        task.setRequestJson(toJson(request));
        task.setParsedRequirementJson(toJson(parsed));
        task.setResultJson(toJson(response));
        task.setStatus(STATUS_SUCCESS);
        task.setRecommendationMode(parsed.getRecommendationMode() == null ? null : parsed.getRecommendationMode().name());
        task.setResultCount(response.getRecommendations() == null ? 0 : response.getRecommendations().size());
        task.setDurationMs(0L);
        recommendationTaskMapper.insert(task);

        saveAiParseLog(task.getId(), requestId, request.getRequirementText(), parsed, parseTrace);
    }

    private void saveAiParseLog(Long taskId,
                                String requestId,
                                String requirementText,
                                ParsedRequirement parsed,
                                AiRequirementParserService.ParseTrace parseTrace) {
        AiParseLog log = new AiParseLog();
        log.setTaskId(taskId);
        log.setRequestId(requestId);
        log.setProvider(parseTrace.provider());
        log.setModelName(parseTrace.modelName());
        log.setParseMode(parseTrace.parseMode());
        log.setSuccessFlag(parseTrace.successFlag());
        log.setRequirementText(requirementText);
        log.setRawResponse(parseTrace.rawResponse());
        log.setParsedJson(toJson(parsed));
        log.setErrorMessage(parseTrace.errorMessage());
        aiParseLogMapper.insert(log);
    }

    private int countRecommendationItems(RecommendationResponse response) {
        int rush = response.getRush() == null ? 0 : response.getRush().size();
        int safe = response.getSafe() == null ? 0 : response.getSafe().size();
        int guarantee = response.getGuarantee() == null ? 0 : response.getGuarantee().size();
        return rush + safe + guarantee;
    }

    private RecommendationTask requiredTask(Long taskId) {
        RecommendationTask task = recommendationTaskMapper.selectOne(
                new LambdaQueryWrapper<RecommendationTask>().eq(RecommendationTask::getId, taskId).last("LIMIT 1"));
        if (task == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Recommendation task not found");
        }
        return task;
    }

    private String buildScoreRawQuery(RecommendationRequest request) {
        return "score=%s, province=%s, subjectType=%s, mode=%s, major=%s".formatted(
                request.getScore(),
                request.getProvince(),
                request.getSubjectType() == null ? null : request.getSubjectType().name(),
                request.getRecommendationMode() == null ? null : request.getRecommendationMode().name(),
                request.getMajorKeyword());
    }

    private String toJson(Object value) {
        if (value == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to serialize tracking payload");
        }
    }

    private <T> T fromJson(String value, Class<T> type) {
        if (value == null || value.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readValue(value, type);
        } catch (JsonProcessingException ex) {
            throw new ResponseStatusException(HttpStatus.INTERNAL_SERVER_ERROR, "Failed to deserialize tracking payload");
        }
    }
}
