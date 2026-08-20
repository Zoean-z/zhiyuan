package com.zhiyuan.college.model.dto;

public record RankLookupResponse(String province,
                                 String subjectType,
                                 Integer mappingYear,
                                 Integer score,
                                 Integer rank,
                                 String rankSource,
                                 String rankSourceLabel) {
}
