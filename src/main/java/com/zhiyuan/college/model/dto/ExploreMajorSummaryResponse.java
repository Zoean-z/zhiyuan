package com.zhiyuan.college.model.dto;

public class ExploreMajorSummaryResponse {

    private final String name;
    private final String code;
    private final String category;
    private final String subcategory;
    private final String duration;
    private final String degree;
    private final String genderRatio;
    private final String averageSalary;
    private final Integer popularity;
    private final Integer offeringSchoolCount;

    public ExploreMajorSummaryResponse(String name, String code, String category, String subcategory,
                                       String duration, String degree, String genderRatio,
                                       String averageSalary, Integer popularity, Integer offeringSchoolCount) {
        this.name = name;
        this.code = code;
        this.category = category;
        this.subcategory = subcategory;
        this.duration = duration;
        this.degree = degree;
        this.genderRatio = genderRatio;
        this.averageSalary = averageSalary;
        this.popularity = popularity;
        this.offeringSchoolCount = offeringSchoolCount;
    }

    public String getName() { return name; }
    public String getCode() { return code; }
    public String getCategory() { return category; }
    public String getSubcategory() { return subcategory; }
    public String getDuration() { return duration; }
    public String getDegree() { return degree; }
    public String getGenderRatio() { return genderRatio; }
    public String getAverageSalary() { return averageSalary; }
    public Integer getPopularity() { return popularity; }
    public Integer getOfferingSchoolCount() { return offeringSchoolCount; }
}
