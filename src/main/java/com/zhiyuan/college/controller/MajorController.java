package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.MajorListResponse;
import com.zhiyuan.college.model.dto.MajorSchoolItemResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.MajorQueryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import java.util.List;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 公开专业目录接口：查专业（32 热门专业 + 开设院校数）、专业的开设院校列表。
 */
@RestController
@RequestMapping("/api/majors")
@Validated
public class MajorController {

    private final MajorQueryService majorQueryService;

    public MajorController(MajorQueryService majorQueryService) {
        this.majorQueryService = majorQueryService;
    }

    @GetMapping
    public MajorListResponse list() {
        return majorQueryService.listMajors();
    }

    @GetMapping("/{majorId}/schools")
    public List<MajorSchoolItemResponse> schools(@PathVariable("majorId") @Positive Long majorId,
                                                 @RequestParam(value = "province", required = false) @Size(max = 20) String province,
                                                 @RequestParam(value = "subjectType", required = false) SubjectType subjectType,
                                                 @RequestParam(value = "score", required = false) @Min(0) @Max(750) Integer score,
                                                 @RequestParam(value = "userRank", required = false) @Positive Integer userRank) {
        return majorQueryService.schoolsOfMajor(
                majorId,
                province,
                subjectType == null ? null : subjectType.getDbValue(),
                score,
                userRank
        );
    }
}
