package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AdminMajorRequest;
import com.zhiyuan.college.model.entity.Major;
import com.zhiyuan.college.service.AdminMajorService;
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
@RequestMapping("/api/admin/majors")
public class AdminMajorController {

    private final AdminMajorService adminMajorService;

    public AdminMajorController(AdminMajorService adminMajorService) {
        this.adminMajorService = adminMajorService;
    }

    @GetMapping
    public List<Major> list() {
        return adminMajorService.list();
    }

    @PostMapping
    public Major create(@Valid @RequestBody AdminMajorRequest request) {
        return adminMajorService.create(request);
    }

    @PutMapping("/{id}")
    public Major update(@PathVariable("id") Long id, @Valid @RequestBody AdminMajorRequest request) {
        return adminMajorService.update(id, request);
    }
}
