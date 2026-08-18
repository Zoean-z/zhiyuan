package com.zhiyuan.college.model.dto;

import java.util.List;

public record UniversityListResponse(int page,
                                     int size,
                                     int total,
                                     String examProvince,
                                     String subjectType,
                                     Integer userRank,
                                     String rankSource,
                                     List<UniversityListItemResponse> items) {
}
