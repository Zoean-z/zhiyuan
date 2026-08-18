package com.zhiyuan.college.service.messaging;

import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;

public record RecommendationTaskMessage(Long taskId,
                                        Long userId,
                                        String requestId,
                                        FreeTextRecommendationRequest request) {
}
