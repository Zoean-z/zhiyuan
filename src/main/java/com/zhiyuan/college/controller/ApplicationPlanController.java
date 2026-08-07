package com.zhiyuan.college.controller;

import com.zhiyuan.college.model.dto.ApplicationPlanCreateRequest;
import com.zhiyuan.college.model.dto.ApplicationPlanDetailResponse;
import com.zhiyuan.college.model.dto.ApplicationPlanRecordResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.security.UserContext;
import com.zhiyuan.college.service.ApplicationPlanService;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/plans")
public class ApplicationPlanController {

    private final ApplicationPlanService applicationPlanService;

    public ApplicationPlanController(ApplicationPlanService applicationPlanService) {
        this.applicationPlanService = applicationPlanService;
    }

    @PostMapping
    public ApplicationPlanDetailResponse save(@Valid @RequestBody ApplicationPlanCreateRequest request) {
        return applicationPlanService.save(currentUserId(), request);
    }

    @GetMapping
    public List<ApplicationPlanRecordResponse> list() {
        return applicationPlanService.listByUser(currentUserId());
    }

    @GetMapping("/current")
    public ApplicationPlanDetailResponse current() {
        return applicationPlanService.getCurrentDraft(currentUserId());
    }

    @PutMapping("/current")
    public ApplicationPlanDetailResponse upsertCurrent(@Valid @RequestBody ApplicationPlanCreateRequest request) {
        return applicationPlanService.upsertCurrentDraft(currentUserId(), request);
    }

    @GetMapping("/{id}")
    public ApplicationPlanDetailResponse detail(@PathVariable("id") Long id) {
        return applicationPlanService.getById(currentUserId(), id);
    }

    @PutMapping("/{id}")
    public ApplicationPlanDetailResponse update(@PathVariable("id") Long id,
                                                @Valid @RequestBody ApplicationPlanCreateRequest request) {
        return applicationPlanService.update(currentUserId(), id, request);
    }

    @DeleteMapping("/current")
    public void deleteCurrent() {
        applicationPlanService.deleteCurrentDraft(currentUserId());
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable("id") Long id) {
        applicationPlanService.deleteById(currentUserId(), id);
    }

    private Long currentUserId() {
        UserAccount user = UserContext.get();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user.getId();
    }
}
