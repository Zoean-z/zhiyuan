package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.AdminUserOverviewResponse;
import com.zhiyuan.college.model.dto.AdminUserResponse;
import com.zhiyuan.college.model.dto.AdminUserSettingsRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.UserRole;
import com.zhiyuan.college.security.UserContext;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class AdminUserService {

    private final UserAccountMapper userAccountMapper;

    public AdminUserService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public List<AdminUserResponse> list(String keyword, UserRole role, Boolean enabled) {
        String normalizedKeyword = keyword == null ? null : keyword.trim();
        return userAccountMapper.findAdminUsers(
                normalizedKeyword,
                role == null ? null : role.name(),
                enabled
        );
    }

    public AdminUserOverviewResponse overview() {
        return userAccountMapper.findAdminUserOverview();
    }

    public AdminUserResponse detail(Long id) {
        AdminUserResponse response = userAccountMapper.findAdminUserById(id);
        if (response == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }
        return response;
    }

    public AdminUserResponse updateSettings(Long id, AdminUserSettingsRequest request) {
        UserAccount existing = userAccountMapper.findByIdCompat(id);
        if (existing == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "User not found");
        }

        UserAccount currentAdmin = UserContext.get();
        if (currentAdmin != null && currentAdmin.getId().equals(id)
                && (request.getRole() != UserRole.ADMIN || !Boolean.TRUE.equals(request.getEnabled()))) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "当前管理员不能停用或降级自己的账号");
        }

        existing.setRole(request.getRole());
        existing.setEnabled(request.getEnabled());
        userAccountMapper.updateById(existing);
        return detail(id);
    }
}
