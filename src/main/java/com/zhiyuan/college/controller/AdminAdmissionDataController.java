package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.AdminAdmissionCutoffRequest;
import com.zhiyuan.college.model.dto.AdminMajorAdmissionCutoffRequest;
import com.zhiyuan.college.model.entity.AdmissionCutoff;
import com.zhiyuan.college.model.entity.MajorAdmissionCutoff;
import com.zhiyuan.college.service.AdminAdmissionDataService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminAdmissionDataController {

    private final AdminAdmissionDataService adminAdmissionDataService;

    public AdminAdmissionDataController(AdminAdmissionDataService adminAdmissionDataService) {
        this.adminAdmissionDataService = adminAdmissionDataService;
    }

    @GetMapping("/admission-cutoffs")
    public List<AdmissionCutoff> listAdmissionCutoffs(@RequestParam(value = "universityId", required = false) Long universityId,
                                                      @RequestParam(value = "province", required = false) String province,
                                                      @RequestParam(value = "subjectType", required = false) String subjectType,
                                                      @RequestParam(value = "admissionYear", required = false) Integer admissionYear) {
        return adminAdmissionDataService.listAdmissionCutoffs(universityId, province, subjectType, admissionYear);
    }

    @PostMapping("/admission-cutoffs")
    public AdmissionCutoff createAdmissionCutoff(@Valid @RequestBody AdminAdmissionCutoffRequest request) {
        return adminAdmissionDataService.createAdmissionCutoff(request);
    }

    @PutMapping("/admission-cutoffs/{id}")
    public AdmissionCutoff updateAdmissionCutoff(@PathVariable("id") Long id,
                                                 @Valid @RequestBody AdminAdmissionCutoffRequest request) {
        return adminAdmissionDataService.updateAdmissionCutoff(id, request);
    }

    @GetMapping("/major-admission-cutoffs")
    public List<MajorAdmissionCutoff> listMajorAdmissionCutoffs(@RequestParam(value = "universityId", required = false) Long universityId,
                                                                @RequestParam(value = "province", required = false) String province,
                                                                @RequestParam(value = "subjectType", required = false) String subjectType,
                                                                @RequestParam(value = "admissionYear", required = false) Integer admissionYear,
                                                                @RequestParam(value = "majorKeyword", required = false) String majorKeyword) {
        return adminAdmissionDataService.listMajorAdmissionCutoffs(universityId, province, subjectType, admissionYear, majorKeyword);
    }

    @PostMapping("/major-admission-cutoffs")
    public MajorAdmissionCutoff createMajorAdmissionCutoff(@Valid @RequestBody AdminMajorAdmissionCutoffRequest request) {
        return adminAdmissionDataService.createMajorAdmissionCutoff(request);
    }

    @PutMapping("/major-admission-cutoffs/{id}")
    public MajorAdmissionCutoff updateMajorAdmissionCutoff(@PathVariable("id") Long id,
                                                           @Valid @RequestBody AdminMajorAdmissionCutoffRequest request) {
        return adminAdmissionDataService.updateMajorAdmissionCutoff(id, request);
    }
}
