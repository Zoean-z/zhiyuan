package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyuan.college.mapper.ApplicationPlanMapper;
import com.zhiyuan.college.model.dto.ApplicationPlanCreateRequest;
import com.zhiyuan.college.model.dto.ApplicationPlanDetailResponse;
import com.zhiyuan.college.model.dto.ApplicationPlanRecordResponse;
import com.zhiyuan.college.model.entity.ApplicationPlan;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationPlanService {

    public static final String SOURCE_TYPE_SCORE = "score";
    public static final String SOURCE_TYPE_TEXT = "text";
    public static final String CURRENT_DRAFT_PLAN_NAME = "当前方案草稿";

    private final ApplicationPlanMapper applicationPlanMapper;
    private final ObjectMapper objectMapper;

    public ApplicationPlanService(ApplicationPlanMapper applicationPlanMapper,
                                  ObjectMapper objectMapper) {
        this.applicationPlanMapper = applicationPlanMapper;
        this.objectMapper = objectMapper;
    }

    public ApplicationPlanDetailResponse save(Long userId, ApplicationPlanCreateRequest request) {
        String sourceType = normalizeSourceType(request.getSourceType());
        ApplicationPlan plan = new ApplicationPlan();
        plan.setUserId(userId);
        plan.setPlanName(trimRequired(request.getPlanName(), "planName is required"));
        plan.setSourceType(sourceType);
        plan.setSourceQuery(trimRequired(request.getSourceQuery(), "sourceQuery is required"));
        plan.setResultJson(normalizeResultJson(request.getResultJson()));
        String aiSummary = trimOptional(request.getAiSummary());
        plan.setAiSummary(aiSummary);
        applicationPlanMapper.insert(plan);
        ApplicationPlan saved = applicationPlanMapper.selectById(plan.getId());
        return toDetailResponse(saved);
    }

    public ApplicationPlanDetailResponse getCurrentDraft(Long userId) {
        ApplicationPlan draft = findCurrentDraftEntity(userId);
        if (draft == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Current draft plan not found");
        }
        return toDetailResponse(draft);
    }

    public ApplicationPlanDetailResponse upsertCurrentDraft(Long userId, ApplicationPlanCreateRequest request) {
        String sourceType = normalizeSourceType(request.getSourceType());
        ApplicationPlan draft = findCurrentDraftEntity(userId);
        if (draft == null) {
            draft = new ApplicationPlan();
            draft.setUserId(userId);
            draft.setPlanName(CURRENT_DRAFT_PLAN_NAME);
        }
        draft.setSourceType(sourceType);
        draft.setSourceQuery(trimRequired(request.getSourceQuery(), "sourceQuery is required"));
        draft.setResultJson(normalizeResultJson(request.getResultJson()));
        draft.setAiSummary(trimOptional(request.getAiSummary()));

        if (draft.getId() == null) {
            applicationPlanMapper.insert(draft);
        } else {
            applicationPlanMapper.updateById(draft);
        }
        return toDetailResponse(applicationPlanMapper.selectById(draft.getId()));
    }

    public void deleteCurrentDraft(Long userId) {
        ApplicationPlan draft = findCurrentDraftEntity(userId);
        if (draft != null) {
            applicationPlanMapper.deleteById(draft.getId());
        }
    }

    public List<ApplicationPlanRecordResponse> listByUser(Long userId) {
        LambdaQueryWrapper<ApplicationPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationPlan::getUserId, userId);
        return applicationPlanMapper.selectList(wrapper).stream()
                .sorted(Comparator
                        .comparing((ApplicationPlan plan) -> isCurrentDraft(plan.getPlanName())).reversed()
                        .thenComparing(ApplicationPlan::getCreatedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                        .thenComparing(ApplicationPlan::getId, Comparator.nullsLast(Comparator.reverseOrder())))
                .map(this::toRecordResponse)
                .toList();
    }

    public ApplicationPlanDetailResponse getById(Long userId, Long id) {
        ApplicationPlan plan = requireOwnedEntity(userId, id);
        return toDetailResponse(plan);
    }

    public ApplicationPlanDetailResponse update(Long userId, Long id, ApplicationPlanCreateRequest request) {
        ApplicationPlan plan = requireOwnedEntity(userId, id);
        plan.setPlanName(trimRequired(request.getPlanName(), "planName is required"));
        plan.setSourceType(normalizeSourceType(request.getSourceType()));
        plan.setSourceQuery(trimRequired(request.getSourceQuery(), "sourceQuery is required"));
        plan.setResultJson(normalizeResultJson(request.getResultJson()));
        plan.setAiSummary(trimOptional(request.getAiSummary()));
        applicationPlanMapper.updateById(plan);
        return toDetailResponse(applicationPlanMapper.selectById(id));
    }

    public void deleteById(Long userId, Long id) {
        ApplicationPlan plan = requireOwnedEntity(userId, id);
        applicationPlanMapper.deleteById(id);
    }

    public ApplicationPlan requireOwnedEntity(Long userId, Long id) {
        if (id == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "planId is required");
        }
        ApplicationPlan plan = applicationPlanMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
        }
        return plan;
    }

    public ApplicationPlan findCurrentDraftEntity(Long userId) {
        LambdaQueryWrapper<ApplicationPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationPlan::getUserId, userId)
                .eq(ApplicationPlan::getPlanName, CURRENT_DRAFT_PLAN_NAME)
                .last("LIMIT 1");
        return applicationPlanMapper.selectOne(wrapper);
    }

    private String normalizeSourceType(String sourceType) {
        if (sourceType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceType is required");
        }
        String normalized = sourceType.trim().toLowerCase();
        if (SOURCE_TYPE_SCORE.equals(normalized) || SOURCE_TYPE_TEXT.equals(normalized)) {
            return normalized;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "sourceType must be score or text");
    }

    private boolean isCurrentDraft(String planName) {
        return CURRENT_DRAFT_PLAN_NAME.equals(planName);
    }

    private String trimRequired(String value, String message) {
        if (value == null || value.trim().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, message);
        }
        return value.trim();
    }

    private String trimOptional(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeResultJson(String resultJson) {
        String normalized = trimRequired(resultJson, "resultJson is required");
        try {
            JsonNode root = objectMapper.readTree(normalized);
            if (root == null || !root.isObject()) {
                throw invalidResultJson();
            }
            for (String group : List.of("rush", "safe", "guarantee")) {
                if (root.has(group) && !root.path(group).isArray()) {
                    throw invalidResultJson();
                }
            }
            return normalized;
        } catch (ResponseStatusException ex) {
            throw ex;
        } catch (Exception ex) {
            throw invalidResultJson();
        }
    }

    private ResponseStatusException invalidResultJson() {
        return new ResponseStatusException(
                HttpStatus.BAD_REQUEST,
                "resultJson must be a JSON object with array groups"
        );
    }

    private ApplicationPlanRecordResponse toRecordResponse(ApplicationPlan plan) {
        return new ApplicationPlanRecordResponse(
                plan.getId(),
                plan.getPlanName(),
                plan.getSourceType(),
                plan.getSourceQuery(),
                plan.getResultJson(),
                plan.getCreatedAt());
    }

    private ApplicationPlanDetailResponse toDetailResponse(ApplicationPlan plan) {
        return new ApplicationPlanDetailResponse(
                plan.getId(),
                plan.getPlanName(),
                plan.getSourceType(),
                plan.getSourceQuery(),
                plan.getResultJson(),
                plan.getAiSummary(),
                plan.getCreatedAt());
    }
}
