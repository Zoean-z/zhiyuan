package com.zhiyuan.college.model.dto;

import jakarta.validation.constraints.NotBlank;

public class AdminUniversityRequest {

    @NotBlank
    private String name;

    @NotBlank
    private String province;

    private String tier;
    private Boolean is985;
    private Boolean is211;
    private Boolean isDoubleFirstClass;
    private String tags;

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
