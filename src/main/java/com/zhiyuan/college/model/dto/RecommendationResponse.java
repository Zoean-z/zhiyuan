package com.zhiyuan.college.model.dto;

import java.util.List;

public class RecommendationResponse {

    private String requestId;
    private Integer userRank;
    private List<RecommendationItemResponse> rush;
    private List<RecommendationItemResponse> safe;
    private List<RecommendationItemResponse> guarantee;
    private String summary;

    public RecommendationResponse(String requestId,
                                  Integer userRank,
                                  List<RecommendationItemResponse> rush,
                                  List<RecommendationItemResponse> safe,
                                  List<RecommendationItemResponse> guarantee,
                                  String summary) {
        this.requestId = requestId;
        this.userRank = userRank;
        this.rush = rush;
        this.safe = safe;
        this.guarantee = guarantee;
        this.summary = summary;
    }

    public String getRequestId() {
        return requestId;
    }

    public Integer getUserRank() {
        return userRank;
    }

    public List<RecommendationItemResponse> getRush() {
        return rush;
    }

    public List<RecommendationItemResponse> getSafe() {
        return safe;
    }

    public List<RecommendationItemResponse> getGuarantee() {
        return guarantee;
    }

    public String getSummary() {
        return summary;
    }
}
