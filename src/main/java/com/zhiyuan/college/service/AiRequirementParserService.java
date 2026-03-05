package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Service;

@Service
public class AiRequirementParserService {

    private static final Pattern SCORE_PATTERN = Pattern.compile("([3-7]\\d{2})");
    private static final List<String> PROVINCES = Arrays.asList(
            "北京", "天津", "上海", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江",
            "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
            "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海",
            "台湾", "内蒙古", "广西", "西藏", "宁夏", "新疆", "香港", "澳门"
    );

    public ParsedRequirement parse(String text) {
        ParsedRequirement parsed = new ParsedRequirement();
        String normalized = text == null ? "" : text.trim();

        Matcher scoreMatcher = SCORE_PATTERN.matcher(normalized);
        if (scoreMatcher.find()) {
            parsed.setScore(Integer.parseInt(scoreMatcher.group(1)));
        }

        List<String> matchedProvinces = new ArrayList<>();
        for (String province : PROVINCES) {
            if (normalized.contains(province)) {
                matchedProvinces.add(province);
            }
        }
        if (!matchedProvinces.isEmpty()) {
            parsed.setCandidateProvince(matchedProvinces.get(0));
            parsed.setSchoolProvince(matchedProvinces.get(matchedProvinces.size() - 1));
        }

        if (normalized.contains("物理") || normalized.contains("理科")) {
            parsed.setSubjectType(SubjectType.PHYSICS);
        } else if (normalized.contains("历史") || normalized.contains("文科")) {
            parsed.setSubjectType(SubjectType.HISTORY);
        }

        if (containsAny(normalized, "保守", "保险", "兜底", "稳上")) {
            parsed.setStrategy(StrategyType.GUARANTEE);
        } else if (containsAny(normalized, "稳定", "稳妥", "求稳", "稳一点")) {
            parsed.setStrategy(StrategyType.SAFE);
        } else if (containsAny(normalized, "冲", "冲刺")) {
            parsed.setStrategy(StrategyType.RUSH);
        }

        return parsed;
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
