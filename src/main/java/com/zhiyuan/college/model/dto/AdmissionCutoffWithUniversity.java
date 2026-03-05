package com.zhiyuan.college.model.dto;

public class AdmissionCutoffWithUniversity {

    private Long id;
    private Long universityId;
    private String universityName;
    private String universityProvince;
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

    public String getUniversityProvince() {
        return universityProvince;
    }

    public void setUniversityProvince(String universityProvince) {
        this.universityProvince = universityProvince;
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
