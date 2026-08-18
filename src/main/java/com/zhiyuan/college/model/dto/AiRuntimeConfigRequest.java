package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AiRuntimeConfigRequest {

    @NotBlank
    private String provider;

    @NotBlank
    private String baseUrl;

    @NotBlank
    private String model;

    private String apiKey;
    private boolean clearApiKey;

    public String getProvider() {
        return provider;
    }

    public void setProvider(String provider) {
        this.provider = provider;
    }

    public String getBaseUrl() {
        return baseUrl;
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = baseUrl;
    }

    public String getModel() {
        return model;
    }

    public void setModel(String model) {
        this.model = model;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public boolean isClearApiKey() {
        return clearApiKey;
    }

    public void setClearApiKey(boolean clearApiKey) {
        this.clearApiKey = clearApiKey;
    }
}
