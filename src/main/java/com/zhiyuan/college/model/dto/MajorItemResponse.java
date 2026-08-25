package com.zhiyuan.college.model.dto;

/**
 * 专业目录单项：专业基本信息 + 开设院校数（来自 major_admission_cutoff 聚合）。
 */
public class MajorItemResponse {

    private Long id;
    private String name;
    private String category;
    private String degreeType;
    private String subjectRequirement;
    private String description;
    private Integer openSchoolCount;

    public MajorItemResponse() {
    }

    public MajorItemResponse(Long id, String name, String category, String degreeType,
                             String subjectRequirement, String description, Integer openSchoolCount) {
        this.id = id;
        this.name = name;
        this.category = category;
        this.degreeType = degreeType;
        this.subjectRequirement = subjectRequirement;
        this.description = description;
        this.openSchoolCount = openSchoolCount;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getCategory() {
        return category;
    }

    public void setCategory(String category) {
        this.category = category;
    }

    public String getDegreeType() {
        return degreeType;
    }

    public void setDegreeType(String degreeType) {
        this.degreeType = degreeType;
    }

    public String getSubjectRequirement() {
        return subjectRequirement;
    }

    public void setSubjectRequirement(String subjectRequirement) {
        this.subjectRequirement = subjectRequirement;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public Integer getOpenSchoolCount() {
        return openSchoolCount;
    }

    public void setOpenSchoolCount(Integer openSchoolCount) {
        this.openSchoolCount = openSchoolCount;
    }
}
