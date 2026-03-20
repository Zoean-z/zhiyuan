package com.zhiyuan.college.model.dto;

public class AdmissionCutoffWithUniversity {

    private Long id;
    private Long universityId;
    private String universityName;
    private String majorName;
    private String universityProvince;
    private String universityTier;
    private Boolean is985;
    private Boolean is211;
    private Boolean isDoubleFirstClass;
    private String universityTags;
    private Integer admissionYear;
    private String province;
    private String subjectType;
    private Integer cutoffScore;
    private Integer minRank;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUniversityId() {
        return universityId;
    }

    public void setUniversityId(Long universityId) {
        this.universityId = universityId;
    }

    public String getUniversityName() {
        return universityName;
    }

    public void setUniversityName(String universityName) {
        this.universityName = universityName;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }

    public String getUniversityProvince() {
        return universityProvince;
    }

    public void setUniversityProvince(String universityProvince) {
        this.universityProvince = universityProvince;
    }

    public String getUniversityTier() {
        return universityTier;
    }

    public void setUniversityTier(String universityTier) {
        this.universityTier = universityTier;
    }

    public Boolean getIs985() {
        return is985;
    }

    public void setIs985(Boolean is985) {
        this.is985 = is985;
    }

    public Boolean getIs211() {
        return is211;
    }

    public void setIs211(Boolean is211) {
        this.is211 = is211;
    }

    public Boolean getIsDoubleFirstClass() {
        return isDoubleFirstClass;
    }

    public void setIsDoubleFirstClass(Boolean isDoubleFirstClass) {
        this.isDoubleFirstClass = isDoubleFirstClass;
    }

    public String getUniversityTags() {
        return universityTags;
    }

    public void setUniversityTags(String universityTags) {
        this.universityTags = universityTags;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getCutoffScore() {
        return cutoffScore;
    }

    public void setCutoffScore(Integer cutoffScore) {
        this.cutoffScore = cutoffScore;
    }

    public Integer getMinRank() {
        return minRank;
    }

    public void setMinRank(Integer minRank) {
        this.minRank = minRank;
    }
}
