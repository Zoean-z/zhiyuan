package com.zhiyuan.college.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("score_rank_mapping")
public class ScoreRankMapping {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("mapping_year")
    private Integer mappingYear;

    private String province;

    @TableField("subject_type")
    private String subjectType;

    private Integer score;

    @TableField("rank_value")
    private Integer rank;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Integer getMappingYear() {
        return mappingYear;
    }

    public void setMappingYear(Integer mappingYear) {
        this.mappingYear = mappingYear;
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

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public Integer getRank() {
        return rank;
    }

    public void setRank(Integer rank) {
        this.rank = rank;
    }
}
