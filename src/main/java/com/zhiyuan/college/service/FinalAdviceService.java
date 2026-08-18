package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.FinalAdviceRequest;
import com.zhiyuan.college.model.dto.FinalAdviceResponse;
import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.enums.StrategyType;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class FinalAdviceService {

    private final RecommendationService recommendationService;
    private final AiAdviceSummaryService aiAdviceSummaryService;

    public FinalAdviceService(RecommendationService recommendationService,
                              AiAdviceSummaryService aiAdviceSummaryService) {
        this.recommendationService = recommendationService;
        this.aiAdviceSummaryService = aiAdviceSummaryService;
    }

    public FinalAdviceResponse generate(FinalAdviceRequest request) {
        RecommendationRequest recommendationRequest = new RecommendationRequest();
        recommendationRequest.setScore(request.getScore());
        recommendationRequest.setProvince(request.getProvince());
        recommendationRequest.setSubjectType(request.getSubjectType());

        RecommendationResponse recommendationResponse = recommendationService.recommend(recommendationRequest);
        StrategyType resolved = resolveStrategy(request.getStrategy());

        List<RecommendationItemResponse> pool = switch (resolved) {
            case RUSH -> recommendationResponse.getRush();
            case SAFE -> recommendationResponse.getSafe();
            case GUARANTEE -> recommendationResponse.getGuarantee();
        };

        List<String> schools = pool.stream()
                .limit(5)
                .map(RecommendationItemResponse::getUniversityName)
                .toList();

        String advice = buildAdvice(request, recommendationRequest, resolved, schools, recommendationResponse.getSummary());
        String summary = aiAdviceSummaryService.summarize(buildAiSummarySource(recommendationRequest, pool, advice));
        return new FinalAdviceResponse(resolved.name(), schools, advice, summary);
    }

    private StrategyType resolveStrategy(String rawStrategy) {
        String text = rawStrategy == null ? "" : rawStrategy;
        String normalized = text.toUpperCase();
        if (normalized.contains("GUARANTEE") || text.contains("保") || text.contains("兜底")) {
            return StrategyType.GUARANTEE;
        }
        if (normalized.contains("RUSH") || text.contains("冲")) {
            return StrategyType.RUSH;
        }
        return StrategyType.SAFE;
    }

    private String buildAdvice(FinalAdviceRequest request,
                               RecommendationRequest resolvedRequest,
                               StrategyType strategy,
                               List<String> schools,
                               String baseSummary) {
        String strategyCn = switch (strategy) {
            case RUSH -> "冲刺";
            case SAFE -> "稳妥";
            case GUARANTEE -> "保底";
        };

        List<String> preferred = request.getPreferredUniversities() == null
                ? List.of() : request.getPreferredUniversities();

        String schoolText = schools.isEmpty()
                ? "当前条件下暂无完全匹配的院校。"
                : "优先关注：" + String.join("、", schools) + "。";

        String preferredText = preferred.isEmpty()
                ? ""
                : "你关注的院校有：" + String.join("、", new ArrayList<>(preferred)) + "。";

        return "最终填报建议（" + strategyCn + "策略）："
                + "建议以"
                + request.getProvince()
                + resolvedRequest.getSubjectType().getDisplayName()
                + "类近年录取位次为主，按冲稳保梯度组合志愿。"
                + schoolText
                + preferredText
                + "正式填报前请核对招生章程、专业限制和近三年位次变化。"
                + "系统摘要：" + baseSummary;
    }

    private String buildAiSummarySource(RecommendationRequest request,
                                        List<RecommendationItemResponse> pool,
                                        String advice) {
        StringBuilder builder = new StringBuilder();
        builder.append("Final advice: ").append(advice).append('\n');
        builder.append("Context: ")
                .append(request.getProvince())
                .append(' ')
                .append(request.getSubjectType().getDisplayName())
                .append(" score ")
                .append(request.getScore())
                .append('\n');
        builder.append("Top schools: ").append(buildPoolDigest(pool));
        return builder.toString();
    }

    private String buildPoolDigest(List<RecommendationItemResponse> pool) {
        if (pool == null || pool.isEmpty()) {
            return "none";
        }
        List<String> parts = new ArrayList<>();
        for (RecommendationItemResponse item : pool.stream().limit(3).toList()) {
            String probability = item.getAdmissionProbability() == null
                    ? "probability unknown"
                    : "probability " + item.getAdmissionProbability() + "%";
            String reason = (item.getMatchReasons() == null || item.getMatchReasons().isEmpty())
                    ? item.getExplanation()
                    : item.getMatchReasons().get(0);
            parts.add(item.getUniversityName()
                    + " | "
                    + (item.getStrategyLabel() == null ? item.getStrategy() : item.getStrategyLabel())
                    + " | "
                    + probability
                    + (reason == null || reason.isBlank() ? "" : " | " + reason));
        }
        return String.join("; ", parts);
    }
}
