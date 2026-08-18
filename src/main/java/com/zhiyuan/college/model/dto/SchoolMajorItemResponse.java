package com.zhiyuan.college.model.dto;

public class SchoolMajorItemResponse {

    private String majorName;
    private Integer cutoffScore;
    private Integer minRank;

    public SchoolMajorItemResponse() {
    }

    public SchoolMajorItemResponse(String majorName, Integer cutoffScore, Integer minRank) {
        this.majorName = majorName;
        this.cutoffScore = cutoffScore;
        this.minRank = minRank;
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
}
