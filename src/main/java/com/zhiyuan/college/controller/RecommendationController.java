package com.zhiyuan.college.controller;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.mapper.MajorAdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.FinalAdviceRequest;
import com.zhiyuan.college.model.dto.FinalAdviceResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.MetaOptionsResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.FinalAdviceService;
import com.zhiyuan.college.service.FreeTextRecommendationService;
import com.zhiyuan.college.service.HistoryService;
import com.zhiyuan.college.service.RecommendationService;
import com.zhiyuan.college.security.UserContext;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
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
    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final MajorAdmissionCutoffMapper majorAdmissionCutoffMapper;
    private final HistoryService historyService;

    public RecommendationController(RecommendationService recommendationService,
                                    FinalAdviceService finalAdviceService,
                                    FreeTextRecommendationService freeTextRecommendationService,
                                    AdmissionCutoffMapper admissionCutoffMapper,
                                    MajorAdmissionCutoffMapper majorAdmissionCutoffMapper,
                                    HistoryService historyService) {
        this.recommendationService = recommendationService;
        this.finalAdviceService = finalAdviceService;
        this.freeTextRecommendationService = freeTextRecommendationService;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.majorAdmissionCutoffMapper = majorAdmissionCutoffMapper;
        this.historyService = historyService;
    }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        RecommendationResponse response = recommendationService.recommend(request);
        historyService.saveScoreHistory(currentUserId(), request, response);
        return response;
    }

    @PostMapping("/recommendations/free-text")
    public FreeTextRecommendationResponse recommendByText(@Valid @RequestBody FreeTextRecommendationRequest request) {
        FreeTextRecommendationResponse response = freeTextRecommendationService.recommend(request);
        historyService.saveTextHistory(currentUserId(), request, response);
        return response;
    }

    @PostMapping("/recommendations/final-advice")
    public FinalAdviceResponse finalAdvice(@Valid @RequestBody FinalAdviceRequest request) {
        return finalAdviceService.generate(request);
    }

    @GetMapping("/meta/options")
    public MetaOptionsResponse getOptions() {
        List<String> provinces = admissionCutoffMapper.findDistinctProvinces();
        List<String> subjectTypes = Arrays.stream(SubjectType.values())
                .map(SubjectType::getDisplayName)
                .toList();
        return new MetaOptionsResponse(provinces, subjectTypes);
    }

    @GetMapping("/meta/major-options")
    public List<String> getMajorOptions(@RequestParam("keyword") String keyword,
                                        @RequestParam(value = "province", required = false) String province,
                                        @RequestParam(value = "subjectType", required = false) SubjectType subjectType) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        if (normalizedKeyword.isBlank()) {
            return List.of();
        }
        return majorAdmissionCutoffMapper.findMajorSuggestions(
                normalizedKeyword,
                province == null || province.isBlank() ? null : province.trim(),
                subjectType == null ? null : subjectType.getDbValue()
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
