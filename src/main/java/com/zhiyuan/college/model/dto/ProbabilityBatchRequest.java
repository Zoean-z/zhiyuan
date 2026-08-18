package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;

public class ProbabilityBatchRequest {

    @Min(value = 0, message = "score must be >= 0")
    @Max(value = 750, message = "score must be <= 750")
    private Integer score;

    @NotBlank(message = "province is required")
    private String province;

    @NotNull(message = "subjectType is required")
    private SubjectType subjectType;

    @Positive(message = "userRank must be positive")
    private Integer userRank;

    @Size(max = 100, message = "universityIds must contain at most 100 items")
    private List<@Positive(message = "universityId must be positive") Long> universityIds;

    @Size(max = 100, message = "universityNames must contain at most 100 items")
    private List<@NotBlank(message = "universityName must not be blank")
            @Size(max = 100, message = "universityName must contain at most 100 characters") String> universityNames;

    @Size(max = 100, message = "majorName must contain at most 100 characters")
    private String majorName;

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getProvince() {
        return province;
    }

    public void setProvince(String province) {
        this.province = province;
    }

    public SubjectType getSubjectType() {
        return subjectType;
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType;
    }

    public Integer getUserRank() {
        return userRank;
    }

    public void setUserRank(Integer userRank) {
        this.userRank = userRank;
    }

    public List<Long> getUniversityIds() {
        return universityIds;
    }

    public void setUniversityIds(List<Long> universityIds) {
        this.universityIds = universityIds;
    }

    public List<String> getUniversityNames() {
        return universityNames;
    }

    public void setUniversityNames(List<String> universityNames) {
        this.universityNames = universityNames;
    }

    public String getMajorName() {
        return majorName;
    }

    public void setMajorName(String majorName) {
        this.majorName = majorName;
    }
}
