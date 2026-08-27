package com.zhiyuan.college.model.dto;

/**
 * 一分一段曲线上的一个点。MyBatis 直接映射，因此保留无参构造与 setter。
 */
public class ScoreRankPointResponse {

    private Integer score;
    private Integer rankValue;
    private Integer segmentCount;

    public ScoreRankPointResponse() {
    }

    public ScoreRankPointResponse(Integer score, Integer rankValue) {
        this.score = score;
        this.rankValue = rankValue;
    }

    public ScoreRankPointResponse(Integer score, Integer rankValue, Integer segmentCount) {
        this.score = score;
        this.rankValue = rankValue;
        this.segmentCount = segmentCount;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRankValue() {
        return rankValue;
    }

    public void setRankValue(Integer rankValue) {
        this.rankValue = rankValue;
    }

    public Integer getSegmentCount() {
        return segmentCount;
    }

    public void setSegmentCount(Integer segmentCount) {
        this.segmentCount = segmentCount;
    }
}
