package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.FreeTextRecommendationRequest;
import com.zhiyuan.college.model.dto.FreeTextRecommendationResponse;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.SubjectType;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.security.UserContext;
import com.zhiyuan.college.service.auth.AuthService;
import com.zhiyuan.college.util.UniversityTagUtils;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class FreeTextRecommendationService {

    private static final int MAX_RECOMMENDATIONS = 8;

    private final AiRequirementParserService parserService;
    private final RecommendationService recommendationService;
    private final AiExplanationService aiExplanationService;
    private final AiAdviceSummaryService aiAdviceSummaryService;
    private final RecommendationHintService recommendationHintService;
    private final AuthService authService;
    private final RecommendationTrackingService recommendationTrackingService;

    public FreeTextRecommendationService(AiRequirementParserService parserService,
                                         RecommendationService recommendationService,
                                         AiExplanationService aiExplanationService,
                                         AiAdviceSummaryService aiAdviceSummaryService,
                                         RecommendationHintService recommendationHintService,
                                         AuthService authService,
                                         RecommendationTrackingService recommendationTrackingService) {
        this.parserService = parserService;
        this.recommendationService = recommendationService;
        this.aiExplanationService = aiExplanationService;
        this.aiAdviceSummaryService = aiAdviceSummaryService;
        this.recommendationHintService = recommendationHintService;
        this.authService = authService;
        this.recommendationTrackingService = recommendationTrackingService;
    }

    public FreeTextRecommendationResponse recommend(FreeTextRecommendationRequest request) {
        String requestId = UUID.randomUUID().toString();
        ExecutionResult execution = execute(request, requestId, UserContext.get());
        UserAccount currentUser = execution.currentUser();
        recommendationTrackingService.saveTextTask(
                currentUser == null ? null : currentUser.getId(),
                requestId,
                request,
                execution.parsedRequirement(),
                execution.response(),
                execution.parseTrace());
        return execution.response();
    }

    public ExecutionResult execute(FreeTextRecommendationRequest request,
                                   String requestId,
                                   UserAccount currentUser) {
        AiRequirementParserService.ParseResult parseResult = parserService.parseWithTrace(request.getRequirementText());
        ParsedRequirement parsed = parseResult.parsedRequirement();
        if (currentUser != null) {
            if (parsed.getScore() == null) {
                if (currentUser.getScore() == null) {
                    throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Score is required");
                }
                parsed.setScore(currentUser.getScore());
            } else if (!parsed.getScore().equals(currentUser.getScore())) {
                authService.updateScore(currentUser.getId(), parsed.getScore());
            }
            if (currentUser.getSubjectType() != null && parsed.getSubjectType() == null) {
                parsed.setSubjectType(currentUser.getSubjectType());
            }
            // Backfill only: an explicit province in the request text ("我是山东考生") must win over the
            // province stored on the profile, otherwise the whole request is scored for the wrong province.
            if (currentUser.getExamProvince() != null && !currentUser.getExamProvince().isBlank()
                    && (parsed.getCandidateProvince() == null || parsed.getCandidateProvince().isBlank())) {
                parsed.setCandidateProvince(currentUser.getExamProvince());
            }
        }
        if (parsed.getScore() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "score is required");
        }
        if (parsed.getSubjectType() == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "subjectType is required");
        }
        if (parsed.getCandidateProvince() == null || parsed.getCandidateProvince().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "examProvince is required");
        }
        RecommendationRequest recommendationRequest = buildRecommendationRequest(parsed);
        RecommendationResponse response = recommendationService.recommend(recommendationRequest);
        List<RecommendationItemResponse> result = filterAndFlatten(response, parsed);
        if (result.size() > MAX_RECOMMENDATIONS) {
            result = new ArrayList<>(result.subList(0, MAX_RECOMMENDATIONS));
        }

        String summary = buildSummary(parsed, recommendationRequest, response.getUserRank(), result);
        String finalAdvice = buildFinalAdvice(parsed, result, summary);
        String aiSummary = aiAdviceSummaryService.summarize(buildAiSummarySource(parsed, result, summary, finalAdvice));
        List<String> tips = recommendationHintService.buildTips(parsed, result.size());
        FreeTextRecommendationResponse finalResponse =
                new FreeTextRecommendationResponse(requestId, parsed, result, summary, finalAdvice, aiSummary, tips);
        return new ExecutionResult(currentUser, parsed, finalResponse, parseResult.parseTrace());
    }

    private RecommendationRequest buildRecommendationRequest(ParsedRequirement parsed) {
        RecommendationRequest request = new RecommendationRequest();
        request.setScore(parsed.getScore());
        request.setProvince(parsed.getCandidateProvince());
        request.setSubjectType(parsed.getSubjectType() == null ? SubjectType.PHYSICS : parsed.getSubjectType());
        request.setRecommendationMode(parsed.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST
                : parsed.getRecommendationMode());
        if (request.getRecommendationMode() == RecommendationMode.MAJOR_FIRST) {
            List<String> preferredMajors = !parsed.getNormalizedMajors().isEmpty()
                    ? parsed.getNormalizedMajors()
                    : parsed.getMajorKeywords();
            if (preferredMajors.isEmpty()) {
                request.setRecommendationMode(RecommendationMode.SCHOOL_FIRST);
                parsed.setRecommendationMode(RecommendationMode.SCHOOL_FIRST);
            } else {
                request.setMajorKeyword(preferredMajors.get(0));
            }
        }
        return request;
    }

    private List<RecommendationItemResponse> filterAndFlatten(RecommendationResponse response, ParsedRequirement parsed) {
        if (response.getRecommendationMode() == RecommendationMode.MAJOR_FIRST) {
            return filterMajorFirstRecommendations(parsed, response.getUserRank());
        }
        List<RecommendationItemResponse> result = new ArrayList<>();
        if (shouldKeepStrategy(parsed, StrategyType.RUSH)) {
            result.addAll(filterItems(response.getRush(), parsed));
        }
        if (shouldKeepStrategy(parsed, StrategyType.SAFE)) {
            result.addAll(filterItems(response.getSafe(), parsed));
        }
        if (shouldKeepStrategy(parsed, StrategyType.GUARANTEE)) {
            result.addAll(filterItems(response.getGuarantee(), parsed));
        }
        return result;
    }

    private List<RecommendationItemResponse> filterMajorFirstRecommendations(ParsedRequirement parsed, Integer userRank) {
        List<String> preferredMajors = !parsed.getNormalizedMajors().isEmpty()
                ? parsed.getNormalizedMajors()
                : parsed.getMajorKeywords();
        List<RecommendationItemResponse> result = new ArrayList<>();
        List<String> seen = new ArrayList<>();
        for (String majorKeyword : preferredMajors) {
            RecommendationRequest request = new RecommendationRequest();
            request.setScore(parsed.getScore());
            request.setProvince(parsed.getCandidateProvince());
            request.setSubjectType(parsed.getSubjectType() == null ? SubjectType.PHYSICS : parsed.getSubjectType());
            request.setRecommendationMode(RecommendationMode.MAJOR_FIRST);
            request.setMajorKeyword(majorKeyword);
            RecommendationResponse response = recommendationService.recommend(request);
            List<RecommendationItemResponse> merged = new ArrayList<>();
            if (shouldKeepStrategy(parsed, StrategyType.RUSH)) {
                merged.addAll(response.getRush());
            }
            if (shouldKeepStrategy(parsed, StrategyType.SAFE)) {
                merged.addAll(response.getSafe());
            }
            if (shouldKeepStrategy(parsed, StrategyType.GUARANTEE)) {
                merged.addAll(response.getGuarantee());
            }
            for (RecommendationItemResponse item : filterItems(merged, parsed)) {
                String key = item.getUniversityName() + "::" + item.getMajorName() + "::" + item.getStrategy();
                if (!seen.contains(key)) {
                    seen.add(key);
                    result.add(item);
                }
            }
        }
        return result;
    }

    private List<RecommendationItemResponse> filterItems(List<RecommendationItemResponse> items, ParsedRequirement parsed) {
        List<RecommendationItemResponse> result = new ArrayList<>();
        for (RecommendationItemResponse item : items) {
            if (!matchesProvince(item, parsed) || !matchesSchoolLevel(item, parsed) || !matchesSchoolType(item, parsed)) {
                continue;
            }
            result.add(item);
        }
        return result;
    }

    private boolean shouldKeepStrategy(ParsedRequirement parsed, StrategyType strategyType) {
        return parsed.getStrategy() == null || parsed.getStrategy() == strategyType;
    }

    private boolean matchesProvince(RecommendationItemResponse item, ParsedRequirement parsed) {
        return parsed.getProvinces().isEmpty()
                || (item.getUniversityProvince() != null && parsed.getProvinces().contains(item.getUniversityProvince()));
    }

    private boolean matchesSchoolLevel(RecommendationItemResponse item, ParsedRequirement parsed) {
        if (parsed.getSchoolLevels().isEmpty()) {
            return true;
        }
        for (String level : parsed.getSchoolLevels()) {
            if (UniversityTagUtils.matchesSchoolLevel(
                    level,
                    item.getIs985(),
                    item.getIs211(),
                    item.getIsDoubleFirstClass(),
                    item.getUniversityTier())) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSchoolType(RecommendationItemResponse item, ParsedRequirement parsed) {
        if (parsed.getSchoolTypes().isEmpty()) {
            return true;
        }
        String tier = item.getUniversityTier() == null ? "" : item.getUniversityTier();
        String tags = item.getUniversityTags() == null ? "" : item.getUniversityTags();
        String combined = tier + " " + tags;
        for (String schoolType : parsed.getSchoolTypes()) {
            if (matchesSchoolTypeText(schoolType, combined)) {
                return true;
            }
        }
        return false;
    }

    private boolean matchesSchoolTypeText(String schoolType, String combined) {
        return switch (schoolType) {
            case "医药类" -> containsAny(combined, "医药类", "医学院", "医科");
            case "师范类" -> containsAny(combined, "师范类", "师范");
            case "财经类" -> containsAny(combined, "财经类", "财经");
            case "理工类" -> containsAny(combined, "理工类", "工科", "理工");
            case "综合类" -> containsAny(combined, "综合类", "综合");
            case "政法类" -> containsAny(combined, "政法类", "政法");
            case "农林类" -> containsAny(combined, "农林类", "农业", "林业");
            case "语言类" -> containsAny(combined, "语言类", "外国语", "语言");
            case "艺术类" -> containsAny(combined, "艺术类", "艺术", "美术", "音乐");
            default -> combined.contains(schoolType);
        };
    }

    private String buildSummary(ParsedRequirement parsed,
                                RecommendationRequest request,
                                Integer userRank,
                                List<RecommendationItemResponse> result) {
        String summary = aiExplanationService.buildSummary(request, result.size(), userRank, hasRankBasedItem(result));
        List<String> filterTexts = new ArrayList<>();
        if (!parsed.getSchoolLevels().isEmpty()) {
            filterTexts.add("学校层次：" + String.join("、", parsed.getSchoolLevels()));
        }
        if (!parsed.getProvinces().isEmpty()) {
            filterTexts.add("地区：" + String.join("、", parsed.getProvinces()));
        }
        if (!parsed.getMajorKeywords().isEmpty()) {
            filterTexts.add("专业关键词：" + String.join("、", parsed.getMajorKeywords()));
        }
        if (!parsed.getNormalizedMajors().isEmpty()) {
            filterTexts.add("标准专业：" + String.join("、", parsed.getNormalizedMajors()));
        }
        if (!parsed.getSchoolTypes().isEmpty()) {
            filterTexts.add("院校类型：" + String.join("、", parsed.getSchoolTypes()));
        }
        if (parsed.getRiskPreference() != null && !parsed.getRiskPreference().isBlank()) {
            filterTexts.add("风险偏好：" + parsed.getRiskPreference());
        }
        if (filterTexts.isEmpty()) {
            return summary;
        }
        return summary + " 已识别条件为" + String.join("；", filterTexts) + "。";
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
                ? "暂无完全匹配结果"
                : String.join("、", recommendations.stream()
                .limit(5)
                .map(item -> item.getMajorName() == null || item.getMajorName().isBlank()
                        ? item.getUniversityName()
                        : item.getUniversityName() + "-" + item.getMajorName())
                .toList());
        return "最终填报建议：你当前分数为" + scoreText + "分，建议采用" + strategyText
                + "策略，重点关注" + (parsed.getRecommendationMode() == RecommendationMode.MAJOR_FIRST ? "学校专业组合：" : "院校：") + schools
                + "。请按冲稳保梯度组合志愿，并核对招生章程、科目限制和近三年位次。系统建议：" + summary;
    }

    private String buildAiSummarySource(ParsedRequirement parsed,
                                        List<RecommendationItemResponse> recommendations,
                                        String summary,
                                        String finalAdvice) {
        StringBuilder builder = new StringBuilder();
        builder.append("Summary: ").append(summary).append('\n');
        builder.append("Rule advice: ").append(finalAdvice).append('\n');
        if (parsed.getRiskPreference() != null && !parsed.getRiskPreference().isBlank()) {
            builder.append("Risk preference: ").append(parsed.getRiskPreference()).append('\n');
        }
        if (!parsed.getNormalizedMajors().isEmpty()) {
            builder.append("Normalized majors: ").append(String.join(", ", parsed.getNormalizedMajors())).append('\n');
        } else if (!parsed.getMajorKeywords().isEmpty()) {
            builder.append("Major keywords: ").append(String.join(", ", parsed.getMajorKeywords())).append('\n');
        }
        builder.append("Top recommendations: ").append(buildRecommendationDigest(recommendations));
        return builder.toString();
    }

    private String buildRecommendationDigest(List<RecommendationItemResponse> recommendations) {
        if (recommendations == null || recommendations.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (RecommendationItemResponse item : recommendations.stream().limit(3).toList()) {
            String target = item.getMajorName() == null || item.getMajorName().isBlank()
                    ? item.getUniversityName()
                    : item.getUniversityName() + "-" + item.getMajorName();
            String strategy = item.getStrategyLabel() == null || item.getStrategyLabel().isBlank()
                    ? item.getStrategy()
                    : item.getStrategyLabel();
            String probability = item.getAdmissionProbability() == null
                    ? "probability unknown"
                    : "probability " + item.getAdmissionProbability() + "%";
            String reason = (item.getMatchReasons() == null || item.getMatchReasons().isEmpty())
                    ? item.getExplanation()
                    : item.getMatchReasons().get(0);
            parts.add(target + " | " + strategy + " | " + probability + (reason == null || reason.isBlank() ? "" : " | " + reason));
        }
        return String.join("; ", parts);
    }

    private boolean hasRankBasedItem(List<RecommendationItemResponse> items) {
        return items.stream().anyMatch(item -> "RANK".equals(item.getRecommendationBasis()));
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    public record ExecutionResult(UserAccount currentUser,
                                  ParsedRequirement parsedRequirement,
                                  FreeTextRecommendationResponse response,
                                  AiRequirementParserService.ParseTrace parseTrace) {
    }
}
