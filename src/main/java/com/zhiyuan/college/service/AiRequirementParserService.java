package com.zhiyuan.college.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.dto.ParsedRequirement;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.model.enums.StrategyType;
import com.zhiyuan.college.model.enums.SubjectType;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiRequirementParserService {

    private static final Logger log = LoggerFactory.getLogger(AiRequirementParserService.class);

    /** “630分”、“630 分”：最可靠的分数语境。 */
    private static final Pattern SCORE_WITH_UNIT_PATTERN =
            Pattern.compile("(?<![0-9])([3-7]\\d{2})\\s*分(?!钟|之|数线)");
    /** “考了630”、“成绩是630”、“总分630”。 */
    private static final Pattern SCORE_WITH_PREFIX_PATTERN =
            Pattern.compile("(?:考了|考出|考到|成绩|分数|总分|得分|高考)[^0-9]{0,4}([3-7]\\d{2})(?![0-9])");
    /** 最后的回退：独立三位数，且不能是其他量词（600人、600公里、600元 …）。 */
    private static final Pattern SCORE_STANDALONE_PATTERN = Pattern.compile(
            "(?<![0-9])([3-7]\\d{2})(?![0-9])"
                    + "(?!\\s*(?:分钟|公里|千米|米|人|名|位|元|万|块|年|届|所|个|条|页|号|km|KM))");
    private static final int MIN_VALID_SCORE = 300;
    private static final int MAX_VALID_SCORE = 750;
    private static final Pattern MAJOR_PATTERN = Pattern.compile("([\\u4e00-\\u9fa5A-Za-z]{2,12})(?:专业|方向)");
    private static final Pattern DISTANCE_PREFERENCE_PATTERN = Pattern.compile("(离[^，。；,;\\s]{1,8}近)");
    private static final List<String> PROVINCES = Arrays.asList(
            "北京", "天津", "上海", "重庆", "河北", "山西", "辽宁", "吉林", "黑龙江",
            "江苏", "浙江", "安徽", "福建", "江西", "山东", "河南", "湖北", "湖南",
            "广东", "海南", "四川", "贵州", "云南", "陕西", "甘肃", "青海",
            "台湾", "内蒙古", "广西", "西藏", "宁夏", "新疆", "香港", "澳门"
    );
    private static final List<String> SCHOOL_LEVELS = Arrays.asList("985", "211", "双一流", "普通");
    private static final Map<String, List<String>> SCHOOL_TYPE_ALIASES = Map.of(
            "医药类", List.of("医药类", "医学院校", "医学院", "医科大学"),
            "师范类", List.of("师范类", "师范学校", "师范大学", "师范院校"),
            "财经类", List.of("财经类", "财经学校", "财经大学"),
            "理工类", List.of("理工类", "理工学校", "理工大学", "工科院校", "工科大学"),
            "综合类", List.of("综合类"),
            "政法类", List.of("政法类", "政法大学", "政法院校"),
            "农林类", List.of("农林类", "农业大学", "林业大学"),
            "语言类", List.of("语言类", "外国语大学", "语言大学"),
            "艺术类", List.of("艺术类", "艺术学院", "美术学院", "音乐学院")
    );
    private static final List<String> MAJOR_KEYWORD_HINTS = Arrays.asList(
            "计算机", "计算机科学与技术", "软件工程", "法学", "电子信息工程", "电子信息",
            "汉语言文学", "汉语言", "人工智能", "临床医学", "医学", "自动化", "通信工程", "金融"
    );
    private static final Map<String, List<String>> MAJOR_NORMALIZATION_MAP = Map.of(
            "计算机", List.of("计算机科学与技术", "软件工程", "网络工程", "信息安全"),
            "电气", List.of("电气工程及其自动化"),
            "医学", List.of("临床医学", "口腔医学", "护理学"),
            "法学", List.of("法学"),
            "师范", List.of("教育学", "汉语言文学（师范）")
    );
    private static final List<String> UNRECOGNIZED_HINTS = Arrays.asList(
            "不要太偏", "名气好一点", "名气好", "就业好", "性价比高", "环境好", "发达地区"
    );

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final RecommendationCacheService recommendationCacheService;
    private final boolean enabled;

    public AiRequirementParserService(AiChatClient aiChatClient,
                                      ObjectMapper objectMapper,
                                      RecommendationCacheService recommendationCacheService,
                                      @Value("${ai.qwen.enabled:true}") boolean enabled) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.recommendationCacheService = recommendationCacheService;
        this.enabled = enabled;
    }

    public ParsedRequirement parse(String text) {
        return parseWithTrace(text).parsedRequirement();
    }

    public ParseResult parseWithTrace(String text) {
        ParseResult cached = recommendationCacheService.getParsedRequirement(text);
        if (cached != null) {
            return cached;
        }

        ParseResult result;
        if (enabled) {
            // Request scoped holder: this service is a singleton, so the raw AI answer must never be
            // kept in a mutable field (concurrent requests would overwrite each other's trace).
            AtomicReference<String> rawAiResponse = new AtomicReference<>();
            try {
                ParsedRequirement aiParsed = parseByAi(text, rawAiResponse);
                if (aiParsed != null) {
                    result = new ParseResult(aiParsed, new ParseTrace(
                            aiChatClient.getProvider(),
                            aiChatClient.getModel(),
                            "AI",
                            true,
                            rawAiResponse.get(),
                            null
                    ));
                    recommendationCacheService.cacheParsedRequirement(text, result);
                    return result;
                }
            } catch (Exception ex) {
                log.warn("Qwen parse failed, fallback to local parser: {}", ex.getMessage());
                ParsedRequirement fallback = parseByRule(text);
                result = new ParseResult(fallback, new ParseTrace(
                        aiChatClient.getProvider(),
                        aiChatClient.getModel(),
                        "RULE_FALLBACK",
                        false,
                        rawAiResponse.get(),
                        ex.getMessage()
                ));
                recommendationCacheService.cacheParsedRequirement(text, result);
                return result;
            }
        }
        ParsedRequirement fallback = parseByRule(text);
        result = new ParseResult(fallback, new ParseTrace(
                enabled ? aiChatClient.getProvider() : "local-rule",
                enabled ? aiChatClient.getModel() : null,
                enabled ? "RULE_ONLY" : "LOCAL_RULE_ONLY",
                true,
                null,
                null
        ));
        recommendationCacheService.cacheParsedRequirement(text, result);
        return result;
    }

    private ParsedRequirement parseByAi(String text, AtomicReference<String> rawResponseHolder) throws Exception {
        String normalizedText = text == null ? "" : text.trim();
        String systemPrompt = """
                你是高考志愿需求解析器。请从用户文本中提取字段，并且只输出 JSON 对象，不要输出任何额外说明。
                JSON 字段：
                - score: 整数，无法识别则为 null
                - recommendationMode: 枚举字符串，值只能是 SCHOOL_FIRST 或 MAJOR_FIRST，无法识别则为 null
                - schoolLevels: 字符串数组，只能包含 985、211、双一流、普通
                - schoolTypes: 字符串数组，只能包含 医药类、师范类、财经类、理工类、综合类、政法类、农林类、语言类、艺术类
                - provinces: 字符串数组，表示目标院校地区偏好，如果是大范围比如华东沿海，离某个地区距离近等，则根据理解输出符合要求的省份
                - majorKeywords: 字符串数组，表示专业关键词
                - normalizedMajors: 字符串数组，表示标准化后的专业名称
                - riskPreference: 字符串，只能是 冲、稳、保，无法识别则为 null
                - unrecognizedPreferences: 字符串数组，表示识别到但暂不执行的偏好
                - candidateProvince: 字符串，考生所在省份，无法识别则为 null
                - schoolProvince: 字符串，目标院校省份，无法识别则为 null
                - subjectType: 枚举字符串，值只能是 PHYSICS 或 HISTORY，无法识别则为 null
                - strategy: 枚举字符串，值只能是 RUSH、SAFE、GUARANTEE，无法识别则为 null
                """;
        String userPrompt = "请解析以下文本：" + normalizedText;

        String aiContent = aiChatClient.chat(systemPrompt, userPrompt, 0.1, true);
        rawResponseHolder.set(aiContent);
        JsonNode root = objectMapper.readTree(aiContent);

        ParsedRequirement parsed = new ParsedRequirement();
        if (root.hasNonNull("score")) {
            parsed.setScore(root.get("score").asInt());
        }
        parsed.setRecommendationMode(resolveRecommendationMode(readNullableText(root, "recommendationMode")));
        parsed.setSchoolLevels(readStringList(root, "schoolLevels"));
        parsed.setSchoolTypes(readStringList(root, "schoolTypes"));
        parsed.setProvinces(readStringList(root, "provinces"));
        parsed.setMajorKeywords(readStringList(root, "majorKeywords"));
        parsed.setNormalizedMajors(readStringList(root, "normalizedMajors"));
        parsed.setRiskPreference(normalizeRiskPreference(readNullableText(root, "riskPreference")));
        parsed.setUnrecognizedPreferences(readStringList(root, "unrecognizedPreferences"));
        parsed.setCandidateProvince(readNullableText(root, "candidateProvince"));
        parsed.setSchoolProvince(readNullableText(root, "schoolProvince"));

        parsed.setSubjectType(resolveSubjectType(readNullableText(root, "subjectType")));
        parsed.setStrategy(resolveStrategyType(readNullableText(root, "strategy")));
        completeDerivedFields(parsed, normalizedText);
        return parsed;
    }

    private ParsedRequirement parseByRule(String text) {
        ParsedRequirement parsed = new ParsedRequirement();
        String normalized = text == null ? "" : text.trim();
        Set<String> schoolLevels = new LinkedHashSet<>();
        Set<String> schoolTypes = new LinkedHashSet<>();
        Set<String> provinces = new LinkedHashSet<>();
        Set<String> majorKeywords = new LinkedHashSet<>();
        Set<String> normalizedMajors = new LinkedHashSet<>();
        Set<String> unrecognizedPreferences = new LinkedHashSet<>();

        Integer parsedScore = extractScore(normalized);
        if (parsedScore != null) {
            parsed.setScore(parsedScore);
        }

        List<String> matchedProvinces = new ArrayList<>();
        for (String province : PROVINCES) {
            if (normalized.contains(province)) {
                matchedProvinces.add(province);
            }
        }
        matchedProvinces.sort((left, right) -> Integer.compare(normalized.indexOf(left), normalized.indexOf(right)));
        provinces.addAll(matchedProvinces);
        if (!provinces.isEmpty()) {
            List<String> provinceList = new ArrayList<>(provinces);
            parsed.setProvinces(provinceList);
            parsed.setCandidateProvince(provinceList.get(0));
            parsed.setSchoolProvince(provinceList.get(provinceList.size() - 1));
        }

        if (normalized.contains("物理") || normalized.contains("理科")) {
            parsed.setSubjectType(SubjectType.PHYSICS);
        } else if (normalized.contains("历史") || normalized.contains("文科")) {
            parsed.setSubjectType(SubjectType.HISTORY);
        }

        if (containsAny(normalized, "保守", "保险", "兜底", "保底", "稳上", "保一点")) {
            parsed.setStrategy(StrategyType.GUARANTEE);
            parsed.setRiskPreference("保");
        } else if (containsAny(normalized, "稳", "求稳", "稳妥", "稳定", "稳一点")) {
            parsed.setStrategy(StrategyType.SAFE);
            parsed.setRiskPreference("稳");
        } else if (containsAny(normalized, "冲", "冲刺")) {
            parsed.setStrategy(StrategyType.RUSH);
            parsed.setRiskPreference("冲");
        }

        for (String level : SCHOOL_LEVELS) {
            if (normalized.contains(level)) {
                schoolLevels.add(level);
            }
        }
        parsed.setSchoolLevels(new ArrayList<>(schoolLevels));

        for (Map.Entry<String, List<String>> entry : SCHOOL_TYPE_ALIASES.entrySet()) {
            if (containsAny(normalized, entry.getValue().toArray(new String[0]))) {
                schoolTypes.add(entry.getKey());
            }
        }
        parsed.setSchoolTypes(new ArrayList<>(schoolTypes));

        for (String keyword : MAJOR_KEYWORD_HINTS) {
            if (normalized.contains(keyword)) {
                majorKeywords.add(keyword);
            }
        }
        Matcher majorMatcher = MAJOR_PATTERN.matcher(normalized);
        while (majorMatcher.find()) {
            String value = normalizeMajorKeyword(majorMatcher.group(1));
            if (value != null && !value.isBlank()) {
                majorKeywords.add(value.trim());
            }
        }
        parsed.setMajorKeywords(new ArrayList<>(majorKeywords));
        normalizedMajors.addAll(standardizeMajorKeywords(parsed.getMajorKeywords()));
        parsed.setNormalizedMajors(new ArrayList<>(normalizedMajors));

        for (String hint : UNRECOGNIZED_HINTS) {
            if (normalized.contains(hint)) {
                unrecognizedPreferences.add(hint);
            }
        }
        Matcher distanceMatcher = DISTANCE_PREFERENCE_PATTERN.matcher(normalized);
        while (distanceMatcher.find()) {
            String value = distanceMatcher.group(1);
            if (value != null && !value.isBlank()) {
                unrecognizedPreferences.add(value.trim());
            }
        }
        parsed.setUnrecognizedPreferences(new ArrayList<>(unrecognizedPreferences));

        completeDerivedFields(parsed, normalized);

        return parsed;
    }

    /**
     * 从自由文本中提取高考分数。优先要求显式语境（“630分”、“考了630”、“成绩630”），
     * 最后才回退到独立三位数；且回退时会排除 600人、600公里、600元 这类非分数量词。
     * 这避开了原来 ([3-7]\\d{2}) 裸正则把任意三位数当成分数的误判。
     */
    private Integer extractScore(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Integer score = firstScoreIn(SCORE_WITH_UNIT_PATTERN, text);
        if (score == null) {
            score = firstScoreIn(SCORE_WITH_PREFIX_PATTERN, text);
        }
        if (score == null) {
            score = firstScoreIn(SCORE_STANDALONE_PATTERN, text);
        }
        return score;
    }

    private Integer firstScoreIn(Pattern pattern, String text) {
        Matcher matcher = pattern.matcher(text);
        while (matcher.find()) {
            int value = Integer.parseInt(matcher.group(1));
            if (value >= MIN_VALID_SCORE && value <= MAX_VALID_SCORE) {
                return value;
            }
        }
        return null;
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

    private List<String> readStringList(JsonNode root, String key) {
        JsonNode node = root.get(key);
        if (node == null || node.isNull() || !node.isArray()) {
            return new ArrayList<>();
        }
        List<String> result = new ArrayList<>();
        for (JsonNode item : node) {
            if (item == null || item.isNull()) {
                continue;
            }
            String value = item.asText().trim();
            if (!value.isEmpty() && !result.contains(value)) {
                result.add(value);
            }
        }
        return result;
    }

    private RecommendationMode resolveRecommendationMode(String value) {
        if (value == null) {
            return null;
        }
        try {
            return RecommendationMode.fromValue(value);
        } catch (Exception ignore) {
            return null;
        }
    }

    private String normalizeRiskPreference(String value) {
        if (value == null) {
            return null;
        }
        if (containsAny(value, "保", "兜底", "保底")) {
            return "保";
        }
        if (containsAny(value, "稳", "稳妥")) {
            return "稳";
        }
        if (containsAny(value, "冲", "冲刺")) {
            return "冲";
        }
        return null;
    }

    private void completeDerivedFields(ParsedRequirement parsed, String originalText) {
        if (parsed.getRiskPreference() == null && parsed.getStrategy() != null) {
            parsed.setRiskPreference(switch (parsed.getStrategy()) {
                case RUSH -> "冲";
                case SAFE -> "稳";
                case GUARANTEE -> "保";
            });
        }
        if (parsed.getStrategy() == null && parsed.getRiskPreference() != null) {
            parsed.setStrategy(switch (parsed.getRiskPreference()) {
                case "冲" -> StrategyType.RUSH;
                case "保" -> StrategyType.GUARANTEE;
                default -> StrategyType.SAFE;
            });
        }
        if ((parsed.getRecommendationMode() == null || parsed.getRecommendationMode() == RecommendationMode.SCHOOL_FIRST)
                && parsed.getMajorKeywords() != null
                && !parsed.getMajorKeywords().isEmpty()) {
            parsed.setRecommendationMode(RecommendationMode.MAJOR_FIRST);
        }
        if (parsed.getRecommendationMode() == null) {
            parsed.setRecommendationMode(RecommendationMode.SCHOOL_FIRST);
        }
        if ((parsed.getSchoolProvince() == null || parsed.getSchoolProvince().isBlank())
                && parsed.getProvinces() != null
                && !parsed.getProvinces().isEmpty()) {
            parsed.setSchoolProvince(parsed.getProvinces().get(0));
        }
        if ((parsed.getCandidateProvince() == null || parsed.getCandidateProvince().isBlank())
                && parsed.getProvinces() != null
                && !parsed.getProvinces().isEmpty()
                && containsAny(originalText == null ? "" : originalText, "考生", "我在", "我是")) {
            parsed.setCandidateProvince(parsed.getProvinces().get(0));
        }
        if (parsed.getCandidateProvince() != null
                && parsed.getProvinces() != null
                && parsed.getProvinces().size() > 1
                && containsAny(originalText == null ? "" : originalText, "考生", "我在", "我是")
                && parsed.getCandidateProvince().equals(parsed.getProvinces().get(0))) {
            parsed.setProvinces(parsed.getProvinces().subList(1, parsed.getProvinces().size()));
        }
        if ((parsed.getSchoolProvince() == null || parsed.getSchoolProvince().isBlank())
                && parsed.getProvinces() != null
                && !parsed.getProvinces().isEmpty()) {
            parsed.setSchoolProvince(parsed.getProvinces().get(0));
        }
        if ((parsed.getNormalizedMajors() == null || parsed.getNormalizedMajors().isEmpty())
                && parsed.getMajorKeywords() != null
                && !parsed.getMajorKeywords().isEmpty()) {
            parsed.setNormalizedMajors(standardizeMajorKeywords(parsed.getMajorKeywords()));
        }
    }

    private String normalizeMajorKeyword(String value) {
        if (value == null) {
            return null;
        }
        String normalized = value.trim();
        String[] prefixes = {"推荐一些", "推荐", "一些", "想学", "想读", "报考", "学习"};
        for (String prefix : prefixes) {
            if (normalized.startsWith(prefix) && normalized.length() > prefix.length()) {
                normalized = normalized.substring(prefix.length()).trim();
            }
        }
        return normalized;
    }

    private List<String> standardizeMajorKeywords(List<String> majorKeywords) {
        Set<String> normalized = new LinkedHashSet<>();
        for (String keyword : majorKeywords) {
            if (keyword == null || keyword.isBlank()) {
                continue;
            }
            for (Map.Entry<String, List<String>> entry : MAJOR_NORMALIZATION_MAP.entrySet()) {
                if (keyword.contains(entry.getKey()) || entry.getKey().contains(keyword)) {
                    normalized.addAll(entry.getValue());
                }
            }
        }
        return new ArrayList<>(normalized);
    }

    public record ParseResult(ParsedRequirement parsedRequirement, ParseTrace parseTrace) {
    }

    public record ParseTrace(String provider,
                             String modelName,
                             String parseMode,
                             Boolean successFlag,
                             String rawResponse,
                             String errorMessage) {
    }
}
