package com.zhiyuan.college.model.dto;

import java.util.List;

public record ScoreRankCurveResponse(String province,
                                    String subjectType,
                                    Integer mappingYear,
                                    int pointCount,
                                    List<ScoreRankPointResponse> points) {
}
