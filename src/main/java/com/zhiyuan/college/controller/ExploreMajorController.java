package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.ExploreMajorDetailResponse;
import com.zhiyuan.college.model.dto.ExploreMajorSummaryResponse;
import com.zhiyuan.college.model.dto.MajorOfferingSchoolResponse;
import com.zhiyuan.college.service.ExploreMajorService;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/explore/majors")
public class ExploreMajorController {

    private final ExploreMajorService exploreMajorService;

    public ExploreMajorController(ExploreMajorService exploreMajorService) {
        this.exploreMajorService = exploreMajorService;
    }

    @GetMapping
    public List<ExploreMajorSummaryResponse> list(
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "category", required = false) String category) {
        return exploreMajorService.list(keyword, category);
    }

    @GetMapping("/{code}")
    public ExploreMajorDetailResponse detail(@PathVariable("code") String code) {
        return exploreMajorService.detail(code);
    }

    @GetMapping("/{code}/schools")
    public List<MajorOfferingSchoolResponse> offeringSchools(@PathVariable("code") String code) {
        return exploreMajorService.offeringSchools(code);
    }
}
