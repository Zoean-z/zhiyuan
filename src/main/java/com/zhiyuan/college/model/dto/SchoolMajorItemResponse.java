package com.zhiyuan.college.model.dto;

public class SchoolMajorItemResponse {

    private String majorName;
    private Integer cutoffScore;
    private Integer minRank;
    private String professionalGroupCode;
    private String professionalGroupName;
    private String primarySubject;
    private String electiveSubjects;

    public SchoolMajorItemResponse() {
    }

    public SchoolMajorItemResponse(String majorName, Integer cutoffScore, Integer minRank) {
        this(majorName, cutoffScore, minRank, null, null, null, null);
    }

    public SchoolMajorItemResponse(String majorName,
                                   Integer cutoffScore,
                                   Integer minRank,
                                   String professionalGroupCode,
                                   String professionalGroupName,
                                   String primarySubject,
                                   String electiveSubjects) {
        this.majorName = majorName;
        this.cutoffScore = cutoffScore;
        this.minRank = minRank;
        this.professionalGroupCode = professionalGroupCode;
        this.professionalGroupName = professionalGroupName;
        this.primarySubject = primarySubject;
        this.electiveSubjects = electiveSubjects;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
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

    public String getProfessionalGroupCode() {
        return professionalGroupCode;
    }

    public void setProfessionalGroupCode(String professionalGroupCode) {
        this.professionalGroupCode = professionalGroupCode;
    }

    public String getProfessionalGroupName() {
        return professionalGroupName;
    }

    public void setProfessionalGroupName(String professionalGroupName) {
        this.professionalGroupName = professionalGroupName;
    }

    public String getPrimarySubject() {
        return primarySubject;
    }

    public void setPrimarySubject(String primarySubject) {
        this.primarySubject = primarySubject;
    }

    public String getElectiveSubjects() {
        return electiveSubjects;
    }

    public void setElectiveSubjects(String electiveSubjects) {
        this.electiveSubjects = electiveSubjects;
    }
}
