package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FreeTextRecommendationService {

    private static final int MAX_RECOMMENDATIONS = 8;

    private final AiRequirementParserService parserService;
    private final AdmissionCutoffMapper admissionCutoffMapper;
    private final AiExplanationService aiExplanationService;
    private final AiAdviceSummaryService aiAdviceSummaryService;

    public FreeTextRecommendationService(AiRequirementParserService parserService,
                                         AdmissionCutoffMapper admissionCutoffMapper,
                                         AiExplanationService aiExplanationService,
                                         AiAdviceSummaryService aiAdviceSummaryService) {
        this.parserService = parserService;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.aiExplanationService = aiExplanationService;
        this.aiAdviceSummaryService = aiAdviceSummaryService;
    }

    public FreeTextRecommendationResponse recommend(FreeTextRecommendationRequest request) {
        ParsedRequirement parsed = parserService.parse(request.getRequirementText());
        if (parsed.getScore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到分数，请在文本中包含分数信息。");
        }
        if (parsed.getCandidateProvince() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "未识别到省份，请在文本中包含省份信息。");
        }

        List<AdmissionCutoffWithUniversity> cutoffs = admissionCutoffMapper.findLatestByProvince(parsed.getCandidateProvince());
        if (cutoffs.isEmpty()) {
            String finalAdvice = "当前条件下暂无可推荐院校数据，建议放宽地区或策略后重试。";
            return new FreeTextRecommendationResponse(
                    parsed,
                    List.of(),
                    "当前条件下暂无可推荐院校数据。",
                    finalAdvice,
                    aiAdviceSummaryService.summarize(finalAdvice)
            );
        }

        List<RecommendationItemResponse> result = new ArrayList<>();
        for (AdmissionCutoffWithUniversity cutoff : cutoffs) {
            if (parsed.getSubjectType() != null && !parsed.getSubjectType().getDbValue().equals(cutoff.getSubjectType())) {
                continue;
            }
            if (parsed.getSchoolProvince() != null && !parsed.getSchoolProvince().equals(cutoff.getUniversityProvince())) {
                continue;
            }

            int gap = parsed.getScore() - cutoff.getCutoffScore();
            StrategyType computed = classify(gap);
            if (computed == null) {
                continue;
            }
            if (parsed.getStrategy() != null && parsed.getStrategy() != computed) {
                continue;
            }

            RecommendationItemResponse item = new RecommendationItemResponse(
                    cutoff.getUniversityName(),
                    cutoff.getCutoffScore(),
                    gap,
                    estimateProbability(gap),
                    computed.name(),
                    null
            );
            result.add(item);
        }

        result.sort(Comparator
                .comparing(RecommendationItemResponse::getAdmissionProbability).reversed()
                .thenComparing(RecommendationItemResponse::getScoreGap).reversed());
        if (result.size() > MAX_RECOMMENDATIONS) {
            result = new ArrayList<>(result.subList(0, MAX_RECOMMENDATIONS));
        }

        RecommendationRequest aiReq = new RecommendationRequest();
        aiReq.setScore(parsed.getScore());
        aiReq.setProvince(parsed.getCandidateProvince());
        aiReq.setSubjectType(parsed.getSubjectType() == null ? SubjectType.PHYSICS : parsed.getSubjectType());
        aiExplanationService.enrichItems(aiReq, result);
        String summary = aiExplanationService.buildSummary(aiReq, result.size());

        String finalAdvice = buildFinalAdvice(parsed, result, summary);
        String aiSummary = aiAdviceSummaryService.summarize(finalAdvice);

        return new FreeTextRecommendationResponse(parsed, result, summary, finalAdvice, aiSummary);
    }

    private String buildFinalAdvice(ParsedRequirement parsed,
                                    List<RecommendationItemResponse> recommendations,
                                    String summary) {
        String strategyText = parsed.getStrategy() == null ? "稳妥" : switch (parsed.getStrategy()) {
            case RUSH -> "冲刺";
            case SAFE -> "稳定";
            case GUARANTEE -> "保守";
        };
        String schools = recommendations.isEmpty()
                ? "暂无完全匹配院校"
                : String.join("、", recommendations.stream().limit(5).map(RecommendationItemResponse::getUniversityName).toList());
        return "最终填报建议：你当前分数为" + parsed.getScore() + "分，建议采用" + strategyText
                + "策略，重点关注院校：" + schools
                + "。请按冲稳保梯度组合志愿，并核对招生章程、科目限制和近三年位次。系统建议：" + summary;
    }

    private StrategyType classify(int gap) {
        if (gap >= -10 && gap <= 5) {
            return StrategyType.RUSH;
        }
        if (gap >= 6 && gap <= 20) {
            return StrategyType.SAFE;
        }
        if (gap >= 21) {
            return StrategyType.GUARANTEE;
        }
        return null;
    }

    private int estimateProbability(int gap) {
        int base = 50 + gap * 2;
        if (gap < 0) {
            base = 40 + gap;
        }
        if (base < 5) {
            return 5;
        }
        return Math.min(base, 99);
    }
}
