package com.zhiyuan.college.model.dto;

public class MajorOfferingSchoolResponse {

    private final Long id;
    private final Integer logoId;
    private final String name;
    private final String province;
    private final String city;
    private final String type;
    private final String nature;
    private final String belong;
    private final Boolean is985;
    private final Boolean is211;
    private final Boolean isDoubleFirstClass;

    public MajorOfferingSchoolResponse(Long id, Integer logoId, String name, String province, String city,
                                       String type, String nature, String belong, Boolean is985,
                                       Boolean is211, Boolean isDoubleFirstClass) {
        this.id = id;
        this.logoId = logoId;
        this.name = name;
        this.province = province;
        this.city = city;
        this.type = type;
        this.nature = nature;
        this.belong = belong;
        this.is985 = is985;
        this.is211 = is211;
        this.isDoubleFirstClass = isDoubleFirstClass;
    }

    public Long getId() { return id; }
    public Integer getLogoId() { return logoId; }
    public String getName() { return name; }
    public String getProvince() { return province; }
    public String getCity() { return city; }
    public String getType() { return type; }
    public String getNature() { return nature; }
    public String getBelong() { return belong; }
    public Boolean getIs985() { return is985; }
    public Boolean getIs211() { return is211; }
    public Boolean getIsDoubleFirstClass() { return isDoubleFirstClass; }
}
