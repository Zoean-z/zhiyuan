package com.zhiyuan.college.model.dto;

import java.util.List;

public record UniversityDetailResponse(Long id,
                                       String name,
                                       String province,
                                       String tier,
                                       Boolean is985,
                                       Boolean is211,
                                       Boolean isDoubleFirstClass,
                                       List<String> schoolTags,
                                       String tags,
                                       String examProvince,
                                       String subjectType,
                                       Integer userRank,
                                       String rankSource,
                                       ProbabilityBreakdownResponse probability,
                                       List<CutoffHistoryItemResponse> cutoffHistory,
                                       List<UniversityMajorItemResponse> majors) {
}
