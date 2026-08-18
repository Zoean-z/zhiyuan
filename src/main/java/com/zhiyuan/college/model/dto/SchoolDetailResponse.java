package com.zhiyuan.college.model.dto;

import java.util.ArrayList;
import java.util.List;

public class SchoolDetailResponse {

    private Long universityId;
    private String universityName;
    private String universityProvince;
    private String universityTier;
    private Boolean is985;
    private Boolean is211;
    private Boolean isDoubleFirstClass;
    private List<String> schoolTags;
    private String universityTags;
    private List<SchoolMajorItemResponse> majors;

    public SchoolDetailResponse(Long universityId,
                                String universityName,
                                String universityProvince,
                                String universityTier,
                                Boolean is985,
                                Boolean is211,
                                Boolean isDoubleFirstClass,
                                List<String> schoolTags,
                                String universityTags,
                                List<SchoolMajorItemResponse> majors) {
        this.universityId = universityId;
        this.universityName = universityName;
        this.universityProvince = universityProvince;
        this.universityTier = universityTier;
        this.is985 = is985;
        this.is211 = is211;
        this.isDoubleFirstClass = isDoubleFirstClass;
        this.schoolTags = schoolTags == null ? new ArrayList<>() : new ArrayList<>(schoolTags);
        this.universityTags = universityTags;
        this.majors = majors == null ? new ArrayList<>() : new ArrayList<>(majors);
    }

    public Long getUniversityId() {
        return universityId;
    }

    public String getUniversityName() {
        return universityName;
    }

    public String getUniversityProvince() {
        return universityProvince;
    }

    public String getUniversityTier() {
        return universityTier;
    }

    public Boolean getIs985() {
        return is985;
    }

    public Boolean getIs211() {
        return is211;
    }

    public Boolean getIsDoubleFirstClass() {
        return isDoubleFirstClass;
    }

    public List<String> getSchoolTags() {
        return schoolTags;
    }

    public String getUniversityTags() {
        return universityTags;
    }

    public List<SchoolMajorItemResponse> getMajors() {
        return majors;
    }
}
