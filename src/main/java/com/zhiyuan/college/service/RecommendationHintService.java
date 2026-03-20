package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.enums.RecommendationMode;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import org.springframework.stereotype.Service;

@Service
public class RecommendationHintService {

    private static final int FEW_RESULT_THRESHOLD = 3;

    public List<String> buildTips(RecommendationRequest request, int resultCount) {
        List<String> relaxableConditions = new ArrayList<>();
        int restrictiveConditionCount = 0;

        if (request.getRecommendationMode() == RecommendationMode.MAJOR_FIRST
                && hasText(request.getMajorKeyword())) {
            relaxableConditions.add("专业");
            restrictiveConditionCount++;
        }

        return buildTips(resultCount, relaxableConditions, restrictiveConditionCount, false);
    }

    public List<String> buildTips(ParsedRequirement parsed, int resultCount) {
        List<String> relaxableConditions = new ArrayList<>();
        int restrictiveConditionCount = 0;

        if (hasStrongSchoolLevel(parsed)) {
            relaxableConditions.add("学校层次");
            restrictiveConditionCount++;
        }
        if (!parsed.getProvinces().isEmpty()) {
            relaxableConditions.add("地区");
            restrictiveConditionCount++;
        }
        if (!parsed.getSchoolTypes().isEmpty()) {
            relaxableConditions.add("院校类型");
            restrictiveConditionCount++;
        }
        if (hasMajorCondition(parsed)) {
            relaxableConditions.add("专业条件");
            restrictiveConditionCount++;
        }
        if (isConservativeRisk(parsed.getRiskPreference())) {
            relaxableConditions.add("风险偏好");
            restrictiveConditionCount++;
        }

        boolean hasStrongCombination = hasStrongSchoolLevel(parsed)
                && !parsed.getProvinces().isEmpty()
                && (!parsed.getSchoolTypes().isEmpty() || hasMajorCondition(parsed));

        return buildTips(resultCount, relaxableConditions, restrictiveConditionCount, hasStrongCombination);
    }

    private List<String> buildTips(int resultCount,
                                   List<String> relaxableConditions,
                                   int restrictiveConditionCount,
                                   boolean hasStrongCombination) {
        LinkedHashSet<String> tips = new LinkedHashSet<>();

        if (resultCount == 0) {
            tips.add("当前条件下暂无合适结果");
            tips.add(buildRelaxSuggestion(relaxableConditions, "建议放宽", "后重试"));
        } else if (resultCount <= FEW_RESULT_THRESHOLD) {
            tips.add("当前筛选条件较严格，仅匹配到少量结果");
            tips.add(buildRelaxSuggestion(relaxableConditions, "可尝试适当放宽", "，以获得更多选择"));
        }

        if (restrictiveConditionCount >= 3 || hasStrongCombination) {
            tips.add("你当前设置了较多筛选条件，结果可能偏少");
            tips.add(buildRelaxSuggestion(relaxableConditions, "建议优先放宽", ""));
        }

        return new ArrayList<>(tips);
    }

    private String buildRelaxSuggestion(List<String> relaxableConditions, String prefix, String suffix) {
        if (relaxableConditions.isEmpty()) {
            return prefix + "筛选条件" + suffix;
        }
        List<String> topConditions = relaxableConditions.size() > 3
                ? relaxableConditions.subList(0, 3)
                : relaxableConditions;
        return prefix + String.join("、", topConditions) + "条件" + suffix;
    }

    private boolean hasStrongSchoolLevel(ParsedRequirement parsed) {
        return parsed.getSchoolLevels().stream()
                .anyMatch(level -> "985".equals(level) || "211".equals(level) || "双一流".equals(level));
    }

    private boolean hasMajorCondition(ParsedRequirement parsed) {
        return !parsed.getNormalizedMajors().isEmpty() || !parsed.getMajorKeywords().isEmpty();
    }

    private boolean isConservativeRisk(String riskPreference) {
        return "稳".equals(riskPreference) || "保".equals(riskPreference);
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isBlank();
    }
}
