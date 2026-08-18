package com.zhiyuan.college.model.dto;

import java.util.List;

public record UniversityFilterOptionsResponse(List<String> schoolProvinces,
                                              List<String> examProvinces,
                                              List<String> subjectTypes,
                                              List<String> levels,
                                              List<String> tags) {
}
