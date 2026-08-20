package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.UniversityDetailResponse;
import com.zhiyuan.college.model.dto.UniversityFilterOptionsResponse;
import com.zhiyuan.college.model.dto.UniversityListResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.UniversityQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开院校接口：查大学列表、院校详情（含近年录取线）、筛选项。
 * 原版只有 /api/admin/** 和需登录的推荐接口，前端查大学只能用本地写死数据，这里补上。
 */
@RestController
@RequestMapping("/api/universities")
@Validated
public class UniversityController {

    private final UniversityQueryService universityQueryService;

    public UniversityController(UniversityQueryService universityQueryService) {
        this.universityQueryService = universityQueryService;
    }

    @GetMapping
    public UniversityListResponse list(@RequestParam(value = "examProvince", required = false) @Size(max = 20) String examProvince,
                                      @RequestParam(value = "subjectType", required = false) SubjectType subjectType,
                                      @RequestParam(value = "province", required = false) @Size(max = 20) String province,
                                      @RequestParam(value = "level", required = false) @Size(max = 40) String level,
                                      @RequestParam(value = "tag", required = false) @Size(max = 40) String tag,
                                      @RequestParam(value = "keyword", required = false) @Size(max = 100) String keyword,
                                      @RequestParam(value = "score", required = false) @Min(0) @Max(750) Integer score,
                                      @RequestParam(value = "userRank", required = false) @Positive Integer userRank,
                                      @RequestParam(value = "sort", required = false) String sort,
                                      @RequestParam(value = "page", required = false, defaultValue = "1") @Min(1) int page,
                                      @RequestParam(value = "size", required = false, defaultValue = "20") @Min(1) @Max(100) int size,
                                      @RequestParam(value = "withDataOnly", required = false, defaultValue = "false") boolean withDataOnly) {
        return universityQueryService.list(
                resolveExamProvince(examProvince),
                resolveSubjectType(subjectType),
                province,
                level,
                tag,
                keyword,
                score,
                userRank,
                sort,
                page,
                size,
                withDataOnly
        );
    }

    @GetMapping("/filters")
    public UniversityFilterOptionsResponse filters() {
        return universityQueryService.filterOptions();
    }

    @GetMapping("/{universityId}")
    public UniversityDetailResponse detail(@PathVariable("universityId") @Positive Long universityId,
                                          @RequestParam(value = "examProvince", required = false) @Size(max = 20) String examProvince,
                                          @RequestParam(value = "subjectType", required = false) SubjectType subjectType,
                                          @RequestParam(value = "score", required = false) @Min(0) @Max(750) Integer score,
                                          @RequestParam(value = "userRank", required = false) @Positive Integer userRank) {
        return universityQueryService.detail(
                universityId,
                resolveExamProvince(examProvince),
                resolveSubjectType(subjectType),
                score,
                userRank
        );
    }

    private String resolveExamProvince(String requested) {
        if (requested != null && !requested.isBlank()) {
            return requested.trim();
        }
        return universityQueryService.defaultExamProvince();
    }

    private String resolveSubjectType(SubjectType requested) {
        return requested == null ? SubjectType.PHYSICS.getDbValue() : requested.getDbValue();
    }
}
