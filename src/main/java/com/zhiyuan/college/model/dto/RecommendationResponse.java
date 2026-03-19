package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.RecommendationMode;
import java.util.List;

public class RecommendationResponse {

    private String requestId;
    private RecommendationMode recommendationMode;
    private Integer userRank;
    private List<RecommendationItemResponse> rush;
    private List<RecommendationItemResponse> safe;
    private List<RecommendationItemResponse> guarantee;
    private String summary;

    public RecommendationResponse(String requestId,
                                  RecommendationMode recommendationMode,
                                  Integer userRank,
                                  List<RecommendationItemResponse> rush,
                                  List<RecommendationItemResponse> safe,
                                  List<RecommendationItemResponse> guarantee,
                                  String summary) {
        this.requestId = requestId;
        this.recommendationMode = recommendationMode;
        this.userRank = userRank;
        this.rush = rush;
        this.safe = safe;
        this.guarantee = guarantee;
        this.summary = summary;
    }

    public String getRequestId() {
        return requestId;
    }

    public RecommendationMode getRecommendationMode() {
        return recommendationMode;
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
