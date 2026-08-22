package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AdminUserOverviewResponse;
import com.zhiyuan.college.model.dto.AdminUserResponse;
import com.zhiyuan.college.model.dto.AdminUserSettingsRequest;
import com.zhiyuan.college.model.enums.UserRole;
import com.zhiyuan.college.service.AdminUserService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/users")
public class AdminUserController {

    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    /**
     * Page through users. {@code page}/{@code size} are optional; omitting them keeps the previous
     * behaviour (first page, at most 200 rows).
     */
    @GetMapping
    public List<AdminUserResponse> list(@RequestParam(value = "keyword", required = false) String keyword,
                                        @RequestParam(value = "role", required = false) UserRole role,
                                        @RequestParam(value = "enabled", required = false) Boolean enabled,
                                        @RequestParam(value = "page", required = false) Integer page,
                                        @RequestParam(value = "size", required = false) Integer size) {
        return adminUserService.list(keyword, role, enabled, page, size);
    }

    @GetMapping("/count")
    public Map<String, Object> count(@RequestParam(value = "keyword", required = false) String keyword,
                                     @RequestParam(value = "role", required = false) UserRole role,
                                     @RequestParam(value = "enabled", required = false) Boolean enabled) {
        return Map.of("total", adminUserService.count(keyword, role, enabled));
    }

    @GetMapping("/overview")
    public AdminUserOverviewResponse overview() {
        return adminUserService.overview();
    }

    @GetMapping("/{id}")
    public AdminUserResponse detail(@PathVariable("id") Long id) {
        return adminUserService.detail(id);
    }

    @PutMapping("/{id}/settings")
    public AdminUserResponse updateSettings(@PathVariable("id") Long id,
                                            @Valid @RequestBody AdminUserSettingsRequest request) {
        return adminUserService.updateSettings(id, request);
    }
}
