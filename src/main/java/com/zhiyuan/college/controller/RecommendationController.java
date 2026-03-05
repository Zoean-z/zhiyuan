package com.zhiyuan.college.controller;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.FinalAdviceRequest;
import com.zhiyuan.college.model.dto.FinalAdviceResponse;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.MetaOptionsResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.service.FinalAdviceService;
import com.zhiyuan.college.service.FreeTextRecommendationService;
import com.zhiyuan.college.service.RecommendationService;
import jakarta.validation.Valid;
import java.util.Arrays;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class RecommendationController {

    private final RecommendationService recommendationService;
    private final FinalAdviceService finalAdviceService;
    private final FreeTextRecommendationService freeTextRecommendationService;
    private final AdmissionCutoffMapper admissionCutoffMapper;

    public RecommendationController(RecommendationService recommendationService,
                                    FinalAdviceService finalAdviceService,
                                    FreeTextRecommendationService freeTextRecommendationService,
                                    AdmissionCutoffMapper admissionCutoffMapper) {
        this.recommendationService = recommendationService;
        this.finalAdviceService = finalAdviceService;
        this.freeTextRecommendationService = freeTextRecommendationService;
        this.admissionCutoffMapper = admissionCutoffMapper;
    }

    @PostMapping("/recommendations")
    public RecommendationResponse recommend(@Valid @RequestBody RecommendationRequest request) {
        return recommendationService.recommend(request);
    }

    @PostMapping("/recommendations/free-text")
    public FreeTextRecommendationResponse recommendByText(@Valid @RequestBody FreeTextRecommendationRequest request) {
        return freeTextRecommendationService.recommend(request);
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
}
