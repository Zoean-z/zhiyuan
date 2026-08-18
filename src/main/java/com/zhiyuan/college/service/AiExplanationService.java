package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.RecommendationItemResponse;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.enums.RecommendationMode;
import java.util.ArrayList;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class AiExplanationService {

    public String buildSummary(RecommendationRequest request, int totalCount, Integer userRank, boolean rankBased) {
        RecommendationMode mode = request.getRecommendationMode() == null
                ? RecommendationMode.SCHOOL_FIRST
                : request.getRecommendationMode();
        String targetText = mode == RecommendationMode.MAJOR_FIRST ? "个学校专业组合" : "所院校";
        if (totalCount == 0) {
            return mode == RecommendationMode.MAJOR_FIRST
                    ? String.format("根据%s%s类考生分数%d与专业关键词“%s”，当前暂无可推荐的学校专业结果。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore(),
                    request.getMajorKeyword() == null ? "-" : request.getMajorKeyword())
                    : String.format("根据%s%s类考生分数%d，当前暂无可推荐院校结果。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore());
        }
        if (rankBased && userRank != null) {
            return String.format(
                    "根据%s%s类考生分数%d对应位次%d，系统按最低录取位次筛选出%d%s，建议结合近年波动综合判断。",
                    request.getProvince(),
                    request.getSubjectType().getDisplayName(),
                    request.getScore(),
                    userRank,
                    totalCount,
                    targetText
            );
        }
        return String.format(
                "根据%s%s类考生分数%d，系统已按最低录取分筛选出%d%s，可作为位次缺失时的兜底参考。",
                request.getProvince(),
                request.getSubjectType().getDisplayName(),
                request.getScore(),
                totalCount,
                targetText
        );
    }

    public String buildItemExplanation(RecommendationRequest request, RecommendationItemResponse itemResponse) {
        List<String> reasons = buildMatchReasons(request, itemResponse);
        if (reasons.isEmpty()) {
            return "当前推荐结果缺少完整的位次或分数信息，可作为补充参考。";
        }
        return String.join("；", reasons) + "。";
    }

    public void enrichItems(RecommendationRequest request, List<RecommendationItemResponse> items) {
        for (int i = 0; i < items.size(); i++) {
            RecommendationItemResponse item = items.get(i);
            item.setStrategyLabel(toStrategyLabel(item.getStrategy()));
            item.setRiskScore(calculateRiskScore(item.getAdmissionProbability()));
            item.setMatchReasons(buildMatchReasons(request, item));
            item.setExplanation(buildItemExplanation(request, item));
            items.set(i, item);
        }
    }

    private List<String> buildMatchReasons(RecommendationRequest request, RecommendationItemResponse item) {
        List<String> reasons = new ArrayList<>();
        String strategyLabel = toStrategyLabel(item.getStrategy());

        if ("RANK".equals(item.getRecommendationBasis())
                && item.getUserRank() != null
                && item.getMinRank() != null
                && item.getRankGap() != null) {
            reasons.add(String.format("你的位次约为 %d，该校近年最低位次约为 %d，位次余量 %d，归为%s档",
                    item.getUserRank(),
                    item.getMinRank(),
                    item.getRankGap(),
                    strategyLabel));
        } else if ("SCORE".equals(item.getRecommendationBasis())
                && item.getCutoffScore() != null
                && item.getScoreGap() != null
                && request.getScore() != null) {
            reasons.add(String.format("你的分数为 %d，该校近年最低分约为 %d，分差 %d，归为%s档",
                    request.getScore(),
                    item.getCutoffScore(),
                    item.getScoreGap(),
                    strategyLabel));
        }

        if (item.getAdmissionProbability() != null) {
            reasons.add(String.format("规则测算录取概率约为 %d%%", item.getAdmissionProbability()));
        }

        if (item.getMajorName() != null
                && !item.getMajorName().isBlank()
                && request.getRecommendationMode() == RecommendationMode.MAJOR_FIRST) {
            reasons.add(String.format("命中专业关键词“%s”，对应专业为 %s",
                    request.getMajorKeyword() == null ? "-" : request.getMajorKeyword(),
                    item.getMajorName()));
        }

        if (item.getSchoolTags() != null && !item.getSchoolTags().isEmpty()) {
            reasons.add("院校标签匹配：" + String.join("/", item.getSchoolTags()));
        }

        if (item.getRiskScore() != null) {
            reasons.add(String.format("风险指数 %d/100", item.getRiskScore()));
        }
        return reasons;
    }

    private String toStrategyLabel(String strategy) {
        if (strategy == null) {
            return "参考";
        }
        return switch (strategy) {
            case "RUSH" -> "冲刺";
            case "SAFE" -> "稳妥";
            case "GUARANTEE" -> "保底";
            default -> strategy;
        };
    }

    private Integer calculateRiskScore(Integer admissionProbability) {
        if (admissionProbability == null) {
            return null;
        }
        return Math.max(0, Math.min(100, 100 - admissionProbability));
    }
}
