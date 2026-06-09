package com.zhiyuan.college.controller;

import com.zhiyuan.college.mapper.MajorMapper;
import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.FinalAdviceRequest;
import com.zhiyuan.college.model.dto.FinalAdviceResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationTaskSubmitResponse;
import com.zhiyuan.college.model.dto.MetaOptionsResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.dto.SchoolDetailResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.AsyncRecommendationTaskService;
import com.zhiyuan.college.service.FinalAdviceService;
import com.zhiyuan.college.service.FreeTextRecommendationService;
import com.zhiyuan.college.service.HistoryService;
import com.zhiyuan.college.service.MetaOptionsService;
import com.zhiyuan.college.service.RecommendationTrackingService;
import com.zhiyuan.college.service.RecommendationService;
import com.zhiyuan.college.service.SchoolDetailService;
import com.zhiyuan.college.security.UserContext;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final FinalAdviceService finalAdviceService;
    private final FreeTextRecommendationService freeTextRecommendationService;
    private final AsyncRecommendationTaskService asyncRecommendationTaskService;
    private final MajorMapper majorMapper;
    private final SchoolDetailService schoolDetailService;
    private final HistoryService historyService;
    private final RecommendationTrackingService recommendationTrackingService;
    private final MetaOptionsService metaOptionsService;

    public RecommendationController(RecommendationService recommendationService,
                                    FinalAdviceService finalAdviceService,
                                    FreeTextRecommendationService freeTextRecommendationService,
                                    AsyncRecommendationTaskService asyncRecommendationTaskService,
                                    MajorMapper majorMapper,
                                    SchoolDetailService schoolDetailService,
                                    HistoryService historyService,
                                    RecommendationTrackingService recommendationTrackingService,
                                    MetaOptionsService metaOptionsService) {
        this.recommendationService = recommendationService;
        this.finalAdviceService = finalAdviceService;
        this.freeTextRecommendationService = freeTextRecommendationService;
        this.asyncRecommendationTaskService = asyncRecommendationTaskService;
        this.majorMapper = majorMapper;
        this.schoolDetailService = schoolDetailService;
        this.historyService = historyService;
        this.recommendationTrackingService = recommendationTrackingService;
        this.metaOptionsService = metaOptionsService;
    }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        RecommendationResponse response = recommendationService.recommend(request);
        Long userId = currentUserId();
        historyService.saveScoreHistory(userId, request, response);
        recommendationTrackingService.saveScoreTask(userId, response.getRequestId(), request, response);
        return response;
    }

    @PostMapping("/recommendations/free-text")
    public FreeTextRecommendationResponse recommendByText(@Valid @RequestBody FreeTextRecommendationRequest request) {
        FreeTextRecommendationResponse response = freeTextRecommendationService.recommend(request);
        historyService.saveTextHistory(currentUserId(), request, response);
        return response;
    }

    @PostMapping("/recommendations/free-text/tasks")
    public FreeTextRecommendationTaskSubmitResponse submitTextRecommendationTask(
            @Valid @RequestBody FreeTextRecommendationRequest request) {
        return asyncRecommendationTaskService.submitTextTask(currentUserId(), request);
    }

    @GetMapping("/recommendations/free-text/tasks/{taskId}")
    public FreeTextRecommendationTaskResponse getTextRecommendationTask(@PathVariable("taskId") Long taskId) {
        return asyncRecommendationTaskService.getTextTask(currentUserId(), taskId);
    }

    @PostMapping("/recommendations/final-advice")
    public FinalAdviceResponse finalAdvice(@Valid @RequestBody FinalAdviceRequest request) {
        return finalAdviceService.generate(request);
    }

    @GetMapping("/meta/options")
    public MetaOptionsResponse getOptions() {
        return metaOptionsService.getOptions();
    }

    @GetMapping("/meta/major-options")
    public List<String> getMajorOptions(@RequestParam("keyword") String keyword,
                                        @RequestParam(value = "province", required = false) String province,
                                        @RequestParam(value = "subjectType", required = false) SubjectType subjectType) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }
        return majorMapper.findSuggestions(
                normalizedKeyword,
                province == null || province.isBlank() ? null : province.trim(),
                subjectType == null ? null : subjectType.getDbValue()
        );
    }

    @GetMapping("/recommendations/schools/{universityId}/majors")
    public SchoolDetailResponse getSchoolDetail(@PathVariable("universityId") Long universityId,
                                                @RequestParam("province") String province,
                                                @RequestParam("subjectType") SubjectType subjectType) {
        if (province == null || province.isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "province is required");
        }
        if (subjectType == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectType is required");
        }

        return schoolDetailService.getSchoolDetail(
                universityId,
                province.trim(),
                subjectType.getDbValue()
        );
    }

    private Long currentUserId() {
        UserAccount user = UserContext.get();
        if (user == null || user.getId() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unauthorized");
        }
        return user.getId();
    }
}
