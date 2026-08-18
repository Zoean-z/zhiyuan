package com.zhiyuan.college.model.dto;

/**
 * 录取概率的完整拆解，前端直接展示，不再自己算。
 */
public record ProbabilityBreakdownResponse(Long universityId,
                                           String universityName,
                                           String majorName,
                                           String province,
                                           String subjectType,
                                           Integer admissionYear,
                                           Integer userScore,
                                           Integer userRank,
                                           String rankSource,
                                           Integer cutoffScore,
                                           Integer minRank,
                                           Integer scoreGap,
                                           Integer rankGap,
                                           Integer rankProbability,
                                           Integer scoreProbability,
                                           Integer probability,
                                           String strategy,
                                           String strategyLabel,
                                           boolean recommended,
                                           double rankWeight,
                                           double scoreWeight,
                                           int minimumProbability,
                                           String explanation) {
}
