package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRequirementParserService {

    private static final Logger log = LoggerFactory.getLogger(AiRequirementParserService.class);

    private static final Pattern SCORE_PATTERN = Pattern.compile("([3-7]\\d{2})");
    private static final List<String> PROVINCES = Arrays.asList(
            "北京", "天津", "上海", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江",
            "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
            "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海",
            "台湾", "内蒙古", "广西", "西藏", "宁夏", "新疆", "香港", "澳门"
    );

    private final QwenAiClient qwenAiClient;
    private final ObjectMapper objectMapper;
    private final boolean enabled;

    public AiRequirementParserService(QwenAiClient qwenAiClient,
                                      ObjectMapper objectMapper,
                                      @Value("${ai.qwen.enabled:true}") boolean enabled) {
        this.qwenAiClient = qwenAiClient;
        this.objectMapper = objectMapper;
        this.enabled = enabled;
    }

    public ParsedRequirement parse(String text) {
        if (enabled) {
            try {
                ParsedRequirement aiParsed = parseByAi(text);
                if (aiParsed != null) {
                    return aiParsed;
                }
            } catch (Exception ex) {
                log.warn("Qwen parse failed, fallback to local parser: {}", ex.getMessage());
            }
        }
        return parseByRule(text);
    }

    private ParsedRequirement parseByAi(String text) throws Exception {
        String normalizedText = text == null ? "" : text.trim();
        String systemPrompt = """
                你是高考志愿需求解析器。请从用户文本中提取字段，并且只输出 JSON 对象，不要输出任何额外说明。
                JSON 字段：
                - score: 整数，无法识别则为 null
                - candidateProvince: 字符串，考生所在省份，无法识别则为 null
                - schoolProvince: 字符串，目标院校省份，无法识别则为 null
                - subjectType: 枚举字符串，值只能是 PHYSICS 或 HISTORY，无法识别则为 null
                - strategy: 枚举字符串，值只能是 RUSH、SAFE、GUARANTEE，无法识别则为 null
                """;
        String userPrompt = "请解析以下文本：" + normalizedText;

        String aiContent = qwenAiClient.chat(systemPrompt, userPrompt, 0.1, true);
        JsonNode root = objectMapper.readTree(aiContent);

        ParsedRequirement parsed = new ParsedRequirement();
        if (root.hasNonNull("score")) {
            parsed.setScore(root.get("score").asInt());
        }
        parsed.setCandidateProvince(readNullableText(root, "candidateProvince"));
        parsed.setSchoolProvince(readNullableText(root, "schoolProvince"));

        parsed.setSubjectType(resolveSubjectType(readNullableText(root, "subjectType")));
        parsed.setStrategy(resolveStrategyType(readNullableText(root, "strategy")));
        return parsed;
    }

    private ParsedRequirement parseByRule(String text) {
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
        } else if (containsAny(normalized, "稳", "求稳", "稳妥", "稳定", "稳一点")) {
            parsed.setStrategy(StrategyType.SAFE);
        } else if (containsAny(normalized, "冲", "冲刺")) {
            parsed.setStrategy(StrategyType.RUSH);
        }

        return parsed;
    }

    private String readNullableText(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull()) {
            return null;
        }
        String value = node.asText().trim();
        return value.isEmpty() ? null : value;
    }

    private SubjectType resolveSubjectType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return SubjectType.fromValue(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private StrategyType resolveStrategyType(String value) {
        if (value == null) {
            return null;
        }
        try {
            return StrategyType.valueOf(value.trim().toUpperCase());
        } catch (Exception ignore) {
            return null;
        }
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
