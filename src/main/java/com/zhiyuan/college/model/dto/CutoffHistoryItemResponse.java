package com.zhiyuan.college.model.dto;

public record CutoffHistoryItemResponse(Integer admissionYear,
                                        String province,
                                        String subjectType,
                                        Integer cutoffScore,
                                        Integer minRank,
                                        Integer scoreDelta,
                                        Integer rankDelta) {
}
