package com.zhiyuan.college.model.dto;

import com.zhiyuan.college.model.enums.UserRole;
import jakarta.validation.constraints.NotNull;

public class AdminUserSettingsRequest {

    @NotNull
    private UserRole role;

    @NotNull
    private Boolean enabled;

    public UserRole getRole() { return role; }
    public void setRole(UserRole role) { this.role = role; }
    public Boolean getEnabled() { return enabled; }
    public void setEnabled(Boolean enabled) { this.enabled = enabled; }
}
