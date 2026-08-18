package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.ProbabilityBatchRequest;
import com.zhiyuan.college.model.dto.ProbabilityBreakdownResponse;
import com.zhiyuan.college.model.dto.ProbabilityRequest;
import com.zhiyuan.college.service.ProbabilityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 录取概率接口。前端（查大学/院校详情/志愿表）统一调这里，不再自己复刻公式。
 */
@RestController
@RequestMapping("/api/probability")
public class ProbabilityController {

    private final ProbabilityService probabilityService;

    public ProbabilityController(ProbabilityService probabilityService) {
        this.probabilityService = probabilityService;
    }

    @PostMapping
    public ProbabilityBreakdownResponse evaluate(@Valid @RequestBody ProbabilityRequest request) {
        return probabilityService.evaluate(request);
    }

    @PostMapping("/batch")
    public List<ProbabilityBreakdownResponse> evaluateBatch(@Valid @RequestBody ProbabilityBatchRequest request) {
        return probabilityService.evaluateBatch(request);
    }
}
