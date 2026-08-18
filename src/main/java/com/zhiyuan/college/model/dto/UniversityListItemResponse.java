package com.zhiyuan.college.model.dto;

import java.util.List;

public record UniversityListItemResponse(Long id,
                                         String name,
                                         String province,
                                         String tier,
                                         Boolean is985,
                                         Boolean is211,
                                         Boolean isDoubleFirstClass,
                                         List<String> schoolTags,
                                         String tags,
                                         Integer admissionYear,
                                         Integer cutoffScore,
                                         Integer minRank,
                                         ProbabilityBreakdownResponse probability) {
}
