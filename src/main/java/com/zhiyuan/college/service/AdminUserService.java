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

    /** Hard ceiling: a crafted size parameter must not be able to dump the whole table. */
    private static final int MAX_PAGE_SIZE = 200;
    private static final int DEFAULT_PAGE_SIZE = 200;

    private final UserAccountMapper userAccountMapper;

    public AdminUserService(UserAccountMapper userAccountMapper) {
        this.userAccountMapper = userAccountMapper;
    }

    public List<AdminUserResponse> list(String keyword, UserRole role, Boolean enabled) {
        return list(keyword, role, enabled, 1, DEFAULT_PAGE_SIZE);
    }

    public List<AdminUserResponse> list(String keyword,
                                        UserRole role,
                                        Boolean enabled,
                                        Integer page,
                                        Integer size) {
        int pageSize = size == null ? DEFAULT_PAGE_SIZE : Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        int pageNumber = page == null ? 1 : Math.max(page, 1);
        int offset = (pageNumber - 1) * pageSize;
        return userAccountMapper.findAdminUsers(
                normalizeKeyword(keyword),
                role == null ? null : role.name(),
                enabled,
                pageSize,
                offset
        );
    }

    /** Total number of rows matching the same filters, so the console can page properly. */
    public long count(String keyword, UserRole role, Boolean enabled) {
        return userAccountMapper.countAdminUsers(
                normalizeKeyword(keyword),
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

    private String normalizeKeyword(String keyword) {
        return keyword == null ? null : keyword.trim();
    }
}
