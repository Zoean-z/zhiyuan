package com.zhiyuan.college.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

@TableName("admission_cutoff")
public class AdmissionCutoff {

    @TableId(type = IdType.AUTO)
    private Long id;

    @TableField("university_id")
    private Long universityId;

    @TableField("admission_year")
    private Integer admissionYear;

    private String province;

    @TableField("subject_type")
    private String subjectType;

    @TableField("cutoff_score")
    private Integer cutoffScore;

    @TableField("min_rank")
    private Integer minRank;

    @TableField(exist = false)
    private String dataKind;

    @TableField(exist = false)
    private String calibrationSource;

    @TableField(exist = false)
    private String simulationRule;

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

    public String getDataKind() {
        return dataKind;
    }

    public void setDataKind(String dataKind) {
        this.dataKind = dataKind;
    }

    public String getCalibrationSource() {
        return calibrationSource;
    }

    public void setCalibrationSource(String calibrationSource) {
        this.calibrationSource = calibrationSource;
    }

    public String getSimulationRule() {
        return simulationRule;
    }

    public void setSimulationRule(String simulationRule) {
        this.simulationRule = simulationRule;
    }
}
