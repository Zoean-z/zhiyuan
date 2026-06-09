package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminMajorRequest {

    @NotBlank
    private String name;

    private String category;

    private String degreeType;

    private String tags;

    private String subjectRequirement;

    private String description;

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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
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
}
