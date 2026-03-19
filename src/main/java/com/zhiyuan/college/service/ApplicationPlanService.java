package com.zhiyuan.college.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.zhiyuan.college.mapper.ApplicationPlanMapper;
import com.zhiyuan.college.model.dto.ApplicationPlanCreateRequest;
import com.zhiyuan.college.model.dto.ApplicationPlanDetailResponse;
import com.zhiyuan.college.model.dto.ApplicationPlanRecordResponse;
import com.zhiyuan.college.model.entity.ApplicationPlan;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ApplicationPlanService {

    public static final String SOURCE_TYPE_SCORE = "score";
    public static final String SOURCE_TYPE_TEXT = "text";

    private final ApplicationPlanMapper applicationPlanMapper;

    public ApplicationPlanService(ApplicationPlanMapper applicationPlanMapper) {
        this.applicationPlanMapper = applicationPlanMapper;
    }

    public ApplicationPlanDetailResponse save(Long userId, ApplicationPlanCreateRequest request) {
        String sourceType = normalizeSourceType(request.getSourceType());
        ApplicationPlan plan = new ApplicationPlan();
        plan.setUserId(userId);
        plan.setPlanName(request.getPlanName().trim());
        plan.setSourceType(sourceType);
        plan.setSourceQuery(request.getSourceQuery().trim());
        plan.setResultJson(request.getResultJson().trim());
        String aiSummary = request.getAiSummary() == null ? "" : request.getAiSummary().trim();
        plan.setAiSummary(aiSummary);
        applicationPlanMapper.insert(plan);
        ApplicationPlan saved = applicationPlanMapper.selectById(plan.getId());
        return toDetailResponse(saved);
    }

    public List<ApplicationPlanRecordResponse> listByUser(Long userId) {
        LambdaQueryWrapper<ApplicationPlan> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(ApplicationPlan::getUserId, userId)
                .orderByDesc(ApplicationPlan::getCreatedAt, ApplicationPlan::getId);
        return applicationPlanMapper.selectList(wrapper).stream()
                .map(this::toRecordResponse)
                .toList();
    }

    public ApplicationPlanDetailResponse getById(Long userId, Long id) {
        ApplicationPlan plan = applicationPlanMapper.selectById(id);
        if (plan == null || !userId.equals(plan.getUserId())) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Plan not found");
        }
        return toDetailResponse(plan);
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

    private ApplicationPlanRecordResponse toRecordResponse(ApplicationPlan plan) {
        return new ApplicationPlanRecordResponse(
                plan.getId(),
                plan.getPlanName(),
                plan.getSourceType(),
                plan.getSourceQuery(),
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
