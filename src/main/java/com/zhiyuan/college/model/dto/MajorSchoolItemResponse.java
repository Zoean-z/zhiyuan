package com.zhiyuan.college.model.dto;

/**
 * 专业开设院校列表响应：开设该专业的院校 + 最新录取线 + 计划数。
 */
public class MajorSchoolItemResponse {

    private Long universityId;
    private String universityName;
    private String schoolType;
    private String nature;
    private String province;
    private String tier;
    private Boolean is985;
    private Boolean is211;
    private Integer cutoffScore;
    private Integer minRank;
    private Integer planCount;
    private Integer admissionYear;
    private String cutoffProvince;
    private String subjectType;
    private String dataKind;
    private String calibrationSource;
    private String simulationRule;
    private ProbabilityBreakdownResponse probability;

    public MajorSchoolItemResponse() {
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

    public String getSchoolType() {
        return schoolType;
    }

    public void setSchoolType(String schoolType) {
        this.schoolType = schoolType;
    }

    public String getNature() {
        return nature;
    }

    public void setNature(String nature) {
        this.nature = nature;
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

    public Integer getPlanCount() {
        return planCount;
    }

    public void setPlanCount(Integer planCount) {
        this.planCount = planCount;
    }

    public Integer getAdmissionYear() {
        return admissionYear;
    }

    public void setAdmissionYear(Integer admissionYear) {
        this.admissionYear = admissionYear;
    }

    public String getCutoffProvince() {
        return cutoffProvince;
    }

    public void setCutoffProvince(String cutoffProvince) {
        this.cutoffProvince = cutoffProvince;
    }

    public String getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(String subjectType) {
        this.subjectType = subjectType;
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

    public ProbabilityBreakdownResponse getProbability() {
        return probability;
    }

    public void setProbability(ProbabilityBreakdownResponse probability) {
        this.probability = probability;
    }
}
