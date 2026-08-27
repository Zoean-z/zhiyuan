package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AiConnectionTestResponse;
import com.zhiyuan.college.model.dto.AiRuntimeConfigRequest;
import com.zhiyuan.college.model.dto.AiRuntimeConfigResponse;
import com.zhiyuan.college.service.AiRuntimeConfigService;
import com.zhiyuan.college.service.AiChatClient;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/ai-config")
public class AdminAiConfigController {

    private final AiRuntimeConfigService configService;
    private final AiChatClient aiChatClient;

    public AdminAiConfigController(AiRuntimeConfigService configService, AiChatClient aiChatClient) {
        this.configService = configService;
        this.aiChatClient = aiChatClient;
    }

    @GetMapping
    public AiRuntimeConfigResponse getConfig() {
        return configService.getForAdmin();
    }

    @PutMapping
    public AiRuntimeConfigResponse updateConfig(@Valid @RequestBody AiRuntimeConfigRequest request) {
        return configService.update(request);
    }

    @PostMapping("/test")
    public AiConnectionTestResponse testConnection(@Valid @RequestBody AiRuntimeConfigRequest request) {
        return aiChatClient.testConnection(request);
    }
}
