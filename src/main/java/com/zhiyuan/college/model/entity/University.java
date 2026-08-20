package com.zhiyuan.college.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;

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
}
