package com.zhiyuan.college.model.dto;

public class AdminUserOverviewResponse {

    private Long totalCount;
    private Long userCount;
    private Long adminCount;
    private Long disabledCount;

    public Long getTotalCount() { return totalCount; }
    public void setTotalCount(Long totalCount) { this.totalCount = totalCount; }
    public Long getUserCount() { return userCount; }
    public void setUserCount(Long userCount) { this.userCount = userCount; }
    public Long getAdminCount() { return adminCount; }
    public void setAdminCount(Long adminCount) { this.adminCount = adminCount; }
    public Long getDisabledCount() { return disabledCount; }
    public void setDisabledCount(Long disabledCount) { this.disabledCount = disabledCount; }
}
