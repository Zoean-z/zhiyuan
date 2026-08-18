package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AdminUniversityRequest;
import com.zhiyuan.college.model.entity.University;
import com.zhiyuan.college.service.AdminUniversityService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/universities")
public class AdminUniversityController {

    private final AdminUniversityService adminUniversityService;

    public AdminUniversityController(AdminUniversityService adminUniversityService) {
        this.adminUniversityService = adminUniversityService;
    }

    @GetMapping
    public List<University> list() {
        return adminUniversityService.list();
    }

    @PostMapping
    public University create(@Valid @RequestBody AdminUniversityRequest request) {
        return adminUniversityService.create(request);
    }

    @PutMapping("/{id}")
    public University update(@PathVariable("id") Long id, @Valid @RequestBody AdminUniversityRequest request) {
        return adminUniversityService.update(id, request);
    }
}
