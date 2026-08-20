package com.zhiyuan.college.model.dto;

public record UniversityMajorItemResponse(String majorName,
                                          Integer admissionYear,
                                          Integer cutoffScore,
                                          Integer minRank,
                                          ProbabilityBreakdownResponse probability) {
}
