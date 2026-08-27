package com.zhiyuan.college.model.dto;

public record CutoffHistoryItemResponse(Integer admissionYear,
                                        String province,
                                        String subjectType,
                                        Integer cutoffScore,
                                        Integer minRank,
                                        String dataKind,
                                        String calibrationSource,
                                        String simulationRule,
                                        Integer scoreDelta,
                                        Integer rankDelta) {
}
