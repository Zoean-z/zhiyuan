package com.zhiyuan.college.model.dto;

import java.util.List;

public class ExploreMajorDetailResponse {

    private final ExploreMajorSummaryResponse major;
    private final String description;
    private final List<String> employmentDirections;
    private final boolean demoData;

    public ExploreMajorDetailResponse(ExploreMajorSummaryResponse major, String description,
                                      List<String> employmentDirections, boolean demoData) {
        this.major = major;
        this.description = description;
        this.employmentDirections = employmentDirections == null ? List.of() : List.copyOf(employmentDirections);
        this.demoData = demoData;
    }

    public ExploreMajorSummaryResponse getMajor() { return major; }
    public String getDescription() { return description; }
    public List<String> getEmploymentDirections() { return employmentDirections; }
    public boolean isDemoData() { return demoData; }
}
