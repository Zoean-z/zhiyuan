package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.UserRole;
import java.time.LocalDateTime;

public class AdminUserResponse {

    private Long id;
    private String username;
    private Integer score;
    private String subjectType;
    private String examProvince;
    private String role;
    private Boolean enabled;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long recommendationCount;
    private Long planCount;
    private Long conversationCount;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    public Integer getScore() { return score; }
    public void setScore(Integer score) { this.score = score; }
    public SubjectType getSubjectType() { return SubjectType.fromValue(subjectType); }
    public void setSubjectType(String subjectType) { this.subjectType = subjectType; }
    public String getExamProvince() { return examProvince; }
    public void setExamProvince(String examProvince) { this.examProvince = examProvince; }
    public UserRole getRole() { return UserRole.fromValue(role); }
    public void setRole(String role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(LocalDateTime updatedAt) { this.updatedAt = updatedAt; }
    public Long getRecommendationCount() { return recommendationCount; }
    public void setRecommendationCount(Long recommendationCount) { this.recommendationCount = recommendationCount; }
    public Long getPlanCount() { return planCount; }
    public void setPlanCount(Long planCount) { this.planCount = planCount; }
    public Long getConversationCount() { return conversationCount; }
    public void setConversationCount(Long conversationCount) { this.conversationCount = conversationCount; }
}
