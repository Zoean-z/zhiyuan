package com.zhiyuan.college.model.dto;

public class FreeTextRecommendationTaskSubmitResponse {

    private Long taskId;
    private String requestId;
    private String status;

    public FreeTextRecommendationTaskSubmitResponse(Long taskId, String requestId, String status) {
        this.taskId = taskId;
        this.requestId = requestId;
        this.status = status;
    }

    public Long getTaskId() {
        return taskId;
    }

    public String getRequestId() {
        return requestId;
    }

    public String getStatus() {
        return status;
    }
}
