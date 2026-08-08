package com.zhiyuan.college.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.UserRole;

@TableName("users")
public class UserAccount {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String username;

    private String password;

    private Integer score;

    @TableField("subject_type")
    private String subjectType;

    @TableField("exam_province")
    private String examProvince;

    private String role;

    private Boolean enabled;

    @TableField("created_at")
    private java.time.LocalDateTime createdAt;

    @TableField("updated_at")
    private java.time.LocalDateTime updatedAt;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public Integer getScore() {
        return score;
    }

    public void setScore(Integer score) {
        this.score = score;
    }

    public String getSubjectTypeValue() {
        return subjectType;
    }

    public void setSubjectTypeValue(String subjectType) {
        this.subjectType = subjectType;
    }

    public SubjectType getSubjectType() {
        return SubjectType.fromValue(subjectType);
    }

    public void setSubjectType(SubjectType subjectType) {
        this.subjectType = subjectType == null ? null : subjectType.getDbValue();
    }

    public String getExamProvince() {
        return examProvince;
    }

    public void setExamProvince(String examProvince) {
        this.examProvince = examProvince;
    }

    public String getRoleValue() {
        return role;
    }

    public void setRoleValue(String role) {
        this.role = role;
    }

    public UserRole getRole() {
        return UserRole.fromValue(role);
    }

    public void setRole(UserRole role) {
        this.role = role == null ? UserRole.USER.name() : role.name();
    }

    public Boolean getEnabled() {
        return enabled;
    }

    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }

    public java.time.LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(java.time.LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public java.time.LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(java.time.LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
}
