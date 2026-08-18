package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AiRuntimeConfigRequest;
import com.zhiyuan.college.model.dto.AiRuntimeConfigResponse;
import com.zhiyuan.college.service.AiRuntimeConfigService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-config")
public class AdminAiConfigController {

    private final AiRuntimeConfigService configService;

    public AdminAiConfigController(AiRuntimeConfigService configService) {
        this.configService = configService;
    }

    @GetMapping
    public AiRuntimeConfigResponse getConfig() {
        return configService.getForAdmin();
    }

    @PutMapping
    public AiRuntimeConfigResponse updateConfig(@Valid @RequestBody AiRuntimeConfigRequest request) {
        return configService.update(request);
    }
}
