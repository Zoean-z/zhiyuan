package com.zhiyuan.college.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.math.BigDecimal;

@TableName("university")
public class University {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    private String province;

    private String tier;

    @TableField("is_985")
    private Boolean is985;

    @TableField("is_211")
    private Boolean is211;

    @TableField("is_double_first_class")
    private Boolean isDoubleFirstClass;

    private String tags;

    private String nature;

    @TableField("school_type")
    private String schoolType;

    @TableField("soft_ranking")
    private Integer softRanking;

    @TableField("postgraduate_rate")
    private BigDecimal postgraduateRate;

    @TableField("has_graduate_school")
    private Boolean hasGraduateSchool;

    @TableField("has_doctor_program")
    private Boolean hasDoctorProgram;

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

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public String getTier() {
        return tier;
    }

    public void setTier(String tier) {
        this.tier = tier;
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

    public String getTags() {
        return tags;
    }

    public void setTags(String tags) {
        this.tags = tags;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
    }

    public String getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(String schoolType) {
        this.schoolType = schoolType;
    }

    public Integer getSoftRanking() {
        return softRanking;
    }

    public void setSoftRanking(Integer softRanking) {
        this.softRanking = softRanking;
    }

    public BigDecimal getPostgraduateRate() {
        return postgraduateRate;
    }

    public void setPostgraduateRate(BigDecimal postgraduateRate) {
        this.postgraduateRate = postgraduateRate;
    }

    public Boolean getHasGraduateSchool() {
        return hasGraduateSchool;
    }

    public void setHasGraduateSchool(Boolean hasGraduateSchool) {
        this.hasGraduateSchool = hasGraduateSchool;
    }

    public Boolean getHasDoctorProgram() {
        return hasDoctorProgram;
    }

    public void setHasDoctorProgram(Boolean hasDoctorProgram) {
        this.hasDoctorProgram = hasDoctorProgram;
    }
}
