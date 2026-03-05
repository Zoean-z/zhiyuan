package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.AdmissionCutoffMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.security.UserContext;
import com.zhiyuan.college.service.auth.AuthService;
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
    private final AuthService authService;

    public FreeTextRecommendationService(AiRequirementParserService parserService,
                                         AdmissionCutoffMapper admissionCutoffMapper,
                                         AiExplanationService aiExplanationService,
                                         AiAdviceSummaryService aiAdviceSummaryService,
                                         AuthService authService) {
        this.parserService = parserService;
        this.admissionCutoffMapper = admissionCutoffMapper;
        this.aiExplanationService = aiExplanationService;
        this.aiAdviceSummaryService = aiAdviceSummaryService;
        this.authService = authService;
    }

    public FreeTextRecommendationResponse recommend(FreeTextRecommendationRequest request) {
        ParsedRequirement parsed = parserService.parse(request.getRequirementText());
        UserAccount currentUser = UserContext.get();
        if (currentUser == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Please login first");
        }

        if (parsed.getScore() == null) {
            if (currentUser.getScore() == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is required at first login");
            }
            parsed.setScore(currentUser.getScore());
        } else if (!parsed.getScore().equals(currentUser.getScore())) {
            authService.updateScore(currentUser.getId(), parsed.getScore());
        }

        List<AdmissionCutoffWithUniversity> cutoffs = loadCandidateCutoffs(parsed);

        if (cutoffs.isEmpty()) {
            String finalAdvice = "当前条件下暂无可推荐院校数据，建议放宽地区或科类条件后重试。";
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

            Integer gap = null;
            Integer probability = null;
            String strategy = null;

            if (parsed.getScore() != null) {
                gap = parsed.getScore() - cutoff.getCutoffScore();
                StrategyType computed = classify(gap);
                if (computed == null) {
                    continue;
                }
                if (parsed.getStrategy() != null && parsed.getStrategy() != computed) {
                    continue;
                }
                probability = estimateProbability(gap);
                strategy = computed.name();
            }

            result.add(new RecommendationItemResponse(
                    cutoff.getUniversityName(),
                    cutoff.getCutoffScore(),
                    gap,
                    probability,
                    strategy,
                    null
            ));
        }

        if (parsed.getScore() != null) {
            result.sort(Comparator
                    .comparing(RecommendationItemResponse::getAdmissionProbability, Comparator.nullsLast(Integer::compareTo)).reversed()
                    .thenComparing(RecommendationItemResponse::getScoreGap, Comparator.nullsLast(Integer::compareTo)).reversed()
                    .thenComparing(RecommendationItemResponse::getUniversityName));
        } else {
            result.sort(Comparator
                    .comparing(RecommendationItemResponse::getCutoffScore, Comparator.nullsLast(Integer::compareTo)).reversed()
                    .thenComparing(RecommendationItemResponse::getUniversityName));
        }

        boolean hasMissingInfo = parsed.getScore() == null
                || parsed.getCandidateProvince() == null
                || parsed.getSubjectType() == null
                || parsed.getSchoolProvince() == null
                || parsed.getStrategy() == null;

        if (!hasMissingInfo && result.size() > MAX_RECOMMENDATIONS) {
            result = new ArrayList<>(result.subList(0, MAX_RECOMMENDATIONS));
        }

        boolean hasAiExplanation = false;
        String summary;
        if (parsed.getScore() != null && parsed.getCandidateProvince() != null) {
            RecommendationRequest aiReq = new RecommendationRequest();
            aiReq.setScore(parsed.getScore());
            aiReq.setProvince(parsed.getCandidateProvince());
            aiReq.setSubjectType(parsed.getSubjectType() == null ? SubjectType.PHYSICS : parsed.getSubjectType());
            aiExplanationService.enrichItems(aiReq, result);
            summary = aiExplanationService.buildSummary(aiReq, result.size());
            hasAiExplanation = result.stream().anyMatch(item ->
                    item.getExplanation() != null && !item.getExplanation().isBlank());
        } else {
            summary = buildGeneralSummary(parsed, result.size());
        }

        String finalAdvice = hasAiExplanation ? "" : buildFinalAdvice(parsed, result, summary);
        String aiSummary = aiAdviceSummaryService.summarize(hasAiExplanation ? summary : finalAdvice);

        return new FreeTextRecommendationResponse(parsed, result, summary, finalAdvice, aiSummary);
    }

    private List<AdmissionCutoffWithUniversity> loadCandidateCutoffs(ParsedRequirement parsed) {
        if (parsed.getCandidateProvince() != null && !parsed.getCandidateProvince().isBlank()) {
            return admissionCutoffMapper.findLatestByProvince(parsed.getCandidateProvince());
        }

        List<AdmissionCutoffWithUniversity> merged = new ArrayList<>();
        for (String province : admissionCutoffMapper.findDistinctProvinces()) {
            merged.addAll(admissionCutoffMapper.findLatestByProvince(province));
        }
        return merged;
    }

    private String buildGeneralSummary(ParsedRequirement parsed, int totalCount) {
        String provinceText = parsed.getCandidateProvince() == null ? "全部省份" : parsed.getCandidateProvince();
        String subjectText = parsed.getSubjectType() == null ? "不限科类" : parsed.getSubjectType().getDisplayName();
        return String.format("根据%s、%s等已提供条件，共匹配到%d所院校。", provinceText, subjectText, totalCount);
    }

    private String buildFinalAdvice(ParsedRequirement parsed,
                                    List<RecommendationItemResponse> recommendations,
                                    String summary) {
        String scoreText = parsed.getScore() == null ? "未知" : parsed.getScore().toString();
        String strategyText = parsed.getStrategy() == null ? "稳妥" : switch (parsed.getStrategy()) {
            case RUSH -> "冲刺";
            case SAFE -> "稳妥";
            case GUARANTEE -> "保守";
        };
        String schools = recommendations.isEmpty()
                ? "暂无完全匹配院校"
                : String.join("、", recommendations.stream().limit(5).map(RecommendationItemResponse::getUniversityName).toList());
        return "最终填报建议：你当前分数为" + scoreText + "分，建议采用" + strategyText
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
