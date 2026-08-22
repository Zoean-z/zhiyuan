package com.zhiyuan.college.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.entity.AgentMessage;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.service.AiChatClient;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentDecisionService {

    private static final Logger log = LoggerFactory.getLogger(AgentDecisionService.class);
    private static final Pattern RECOMMEND_MAJOR_AFTER_PATTERN = Pattern.compile("推荐(?:一下|几个|一些)?([\\p{IsHan}A-Za-z0-9]{2,12})(?:专业|方向)");
    private static final Pattern RECOMMEND_MAJOR_BEFORE_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9]{2,12})(?:专业|方向).{0,8}推荐");
    private static final Pattern DIGIT_SELECTION_PATTERN = Pattern.compile("第\\s*([1-6])\\s*(?:个|所|条)");
    private static final Pattern SAVE_NAME_PATTERN = Pattern.compile("保存(?:为|成)?[《\u201c\\\"]?([^》\u201d\\n]{2,30})[》\u201d\\\"]?(?:方案)?");
    private static final Pattern SCHOOL_NAME_DETAIL_PATTERN = Pattern.compile("([\\p{IsHan}A-Za-z0-9]{2,20}(?:大学|学院|学校))");

    /** 常见专业关键词：精确子串匹配优先于正则，避免"推荐好的计算机专业"捕获到"好的计算机"。 */
    private static final List<String> MAJOR_KEYWORDS = List.of(
            "计算机", "软件", "网络", "信息安全", "法学", "护理",
            "人工智能", "AI", "机器学习", "数据科学", "大数据",
            "师范", "教育学", "汉语言", "数学", "物理",
            "电子信息", "电气工程", "自动化", "通信",
            "临床医学", "口腔医学", "中医学", "药学",
            "金融", "会计", "经济学", "工商管理",
            "机械", "土木", "建筑", "材料",
            "新能源", "集成电路", "芯片", "半导体",
            "医学"
    );

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final AgentToolRegistry agentToolRegistry;
    private final boolean qwenEnabled;

    public AgentDecisionService(AiChatClient aiChatClient,
                                ObjectMapper objectMapper,
                                AgentToolRegistry agentToolRegistry,
                                @Value("${ai.qwen.enabled:true}") boolean qwenEnabled) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.agentToolRegistry = agentToolRegistry;
        this.qwenEnabled = qwenEnabled;
    }

    public AgentDecision decide(String userMessage, List<AgentMessage> recentMessages, UserAccount user) {
        AgentDecision localDecision = decideLocally(userMessage, recentMessages);
        // Strong-intent keywords (recommend / profile / plan / delete / save / school
        // detail) are resolved locally with high precision. Let them short-circuit so
        // the tool actually runs instead of being bypassed by an LLM that prefers to
        // reply directly. Only when the local planner has no match do we ask the LLM
        // for semantic understanding of fuzzier requests.
        if (localDecision != null) {
            return localDecision;
        }
        if (!qwenEnabled) {
            return defaultReply();
        }
        try {
            String aiContent = aiChatClient.chat(
                    buildSystemPrompt(),
                    buildUserPrompt(userMessage, recentMessages, user),
                    0.1,
                    true
            );
            JsonNode root = objectMapper.readTree(aiContent);
            String action = root.path("action").asText("").trim();
            String reply = root.path("reply").asText("").trim();
            Map<String, Object> toolArgs = readToolArgs(root.path("toolArgs"));
            if (AgentToolNames.REPLY.equals(action)) {
                return new AgentDecision(AgentToolNames.REPLY, reply.isBlank() ? DEFAULT_REPLY_TEXT : reply);
            }
            if (agentToolRegistry.supports(action)) {
                return new AgentDecision(action, reply, toolArgs);
            }
        } catch (Exception ex) {
            log.warn("Agent AI decision failed, fallback to local planner: {}", ex.getMessage());
        }
        return defaultReply();
    }

    private static final String DEFAULT_REPLY_TEXT =
            "当前 agent 支持查看画像、查看当前志愿方案、生成学校/专业推荐、查看学校详情，也可以把最近推荐里的某一项加入志愿单。删除操作需要你明确确认。";

    private AgentDecision defaultReply() {
        return new AgentDecision(AgentToolNames.REPLY, DEFAULT_REPLY_TEXT);
    }

    private AgentDecision decideLocally(String userMessage, List<AgentMessage> recentMessages) {
        String normalized = userMessage == null ? "" : userMessage.trim();

        // --- #1: removePlanItem 确认删除 (unchanged) ---
        if (containsAny(normalized, "确认删除", "确定删除")) {
            int selectionIndex = extractSelectionIndex(normalized);
            if (hasPendingDeleteConfirmation(recentMessages, selectionIndex)) {
                return new AgentDecision(
                        AgentToolNames.REMOVE_PLAN_ITEM,
                        "我现在删除当前志愿单中的第 %s 个结果。".formatted(selectionIndex),
                        Map.of("selectionIndex", selectionIndex)
                );
            }
            return new AgentDecision(AgentToolNames.REPLY, "我没有检测到最近一条待确认的删除请求，请先明确告诉我要删除哪一项，再按提示确认。");
        }

        // --- P1 #2: 删除提示路由收紧 ---
        // 要求 "删除/移除" + "志愿/方案" + ("当前" 或 序号引用)
        // 排除过去时陈述，避免"我刚把第3条志愿删除了"误触发
        if (containsAny(normalized, "删除", "移除")
                && containsAny(normalized, "志愿", "方案")
                && (containsOrdinalReference(normalized) || containsAny(normalized, "当前"))
                && !containsAny(normalized, "刚删除", "刚移除", "已经删除", "已经移除",
                                "删掉了", "移除了", "刚把", "已经把")) {
            int selectionIndex = extractSelectionIndex(normalized);
            return new AgentDecision(
                    AgentToolNames.REPLY,
                    "删除是敏感操作。若确认删除当前志愿单中的第 %s 个结果，请回复\u201c确认删除第%s个\u201d。".formatted(selectionIndex, selectionIndex)
            );
        }

        // --- #3: savePlan (unchanged) ---
        if (containsAny(normalized, "保存方案", "保存当前方案", "命名保存", "改名保存", "保存为", "另存为")) {
            String planName = extractPlanName(normalized);
            if (planName == null || planName.isBlank()) {
                return new AgentDecision(AgentToolNames.REPLY, "请直接告诉我方案名，例如：保存为\u201c冲稳保方案\u201d。");
            }
            return new AgentDecision(
                    AgentToolNames.SAVE_PLAN,
                    "我现在把当前志愿单保存为《%s》。".formatted(planName),
                    Map.of("planName", planName)
            );
        }

        // --- #4: addPlanItem (unchanged) ---
        if (containsAny(normalized, "加入志愿单", "加入当前方案", "加入方案", "加到志愿单", "加进志愿单")) {
            int selectionIndex = extractSelectionIndex(normalized);
            return new AgentDecision(
                    AgentToolNames.ADD_PLAN_ITEM,
                    "我先把最近推荐里的第 %s 个结果加入当前志愿单。".formatted(selectionIndex),
                    Map.of("selectionIndex", selectionIndex)
            );
        }

        // --- P0 #5: getSchoolDetailByName 收紧 ---
        // 要求 "查看/看看" + 校名 + "详情/信息/专业" 三者同时出现
        // 避免"看看能不能上浙大"（推荐意图）误命中
        String schoolName = extractSchoolName(normalized);
        if (!containsOrdinalReference(normalized)
                && schoolName != null
                && containsAny(normalized, "查看", "看看")
                && containsAny(normalized, "详情", "信息", "专业")) {
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME,
                    "我先按学校名帮你查询\u201c%s\u201d的详情和可参考专业。".formatted(schoolName),
                    Map.of("universityName", schoolName)
            );
        }

        // --- P0 #6: getSchoolDetail 收紧 ---
        // 要求 序号引用(第N个) + "详情/信息/专业" 组合，去掉泛词单独触发
        // 避免"什么专业好就业"误命中 selectionIndex 默认1 导致答非所问
        if (containsOrdinalReference(normalized)
                && containsAny(normalized, "学校详情", "院校详情", "学校信息", "学校专业",
                               "有哪些专业", "什么专业", "详情", "信息", "专业")) {
            int selectionIndex = extractSelectionIndex(normalized);
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL,
                    "我先帮你查看第 %s 个学校的详情和可参考专业。".formatted(selectionIndex),
                    Map.of("selectionIndex", selectionIndex)
            );
        }

        // --- P1 #7: recommendMajors（关键词扩展见 extractMajorKeyword） ---
        String majorKeyword = extractMajorKeyword(normalized);
        if (containsAny(normalized, "推荐") && majorKeyword != null) {
            return new AgentDecision(
                    AgentToolNames.RECOMMEND_MAJORS,
                    "我先基于你的画像和\u201c%s\u201d的兴趣给你生成专业推荐。".formatted(majorKeyword),
                    Map.of("majorKeyword", majorKeyword)
            );
        }

        // --- #8: recommendSchools (unchanged) ---
        if (containsAny(normalized, "推荐学校", "学校推荐", "推荐院校", "院校推荐", "学校怎么报") ||
                (containsAny(normalized, "冲稳保") && containsAny(normalized, "志愿", "方案", "推荐", "浓度", "梯度"))) {
            return new AgentDecision(AgentToolNames.RECOMMEND_SCHOOLS, "我先基于你当前画像给你生成学校推荐。");
        }

        // --- P0 #9: getUserProfile 路由收紧 ---
        // 去掉 "分数/省份/科类" 等高频泛词，改为明确问询短语
        // 避免 "我620分想去北京" "我是浙江考生" 等自然语言请求误命中
        if (containsAny(normalized, "我的画像", "查看画像", "查看我的信息", "我的信息是什么", "我的信息有哪些",
                "我是什么科类", "我的科类", "我的分数是多少", "我的分数",
                "我是哪个省份", "我的省份", "我的考生信息")
                && !containsAny(normalized, "修改", "更新", "编辑", "完善", "设置")) {
            return new AgentDecision(AgentToolNames.GET_USER_PROFILE, "我先帮你读取当前画像信息。");
        }

        // --- #10: getCurrentPlan (unchanged) ---
        // 必须含明确"查看/现有"语境，避免被"生成方案""冲稳保方案"等含"志愿/方案"的请求误触发
        if (containsAny(normalized, "当前表", "当前单", "当前志愿", "当前方案", "我的志愿", "我的方案",
                        "已有志愿", "已有方案", "看看志愿", "看看方案", "之前生成", "刚才生成") ||
                (containsAny(normalized, "当前") && containsAny(normalized, "志愿", "方案"))) {
            return new AgentDecision(AgentToolNames.GET_CURRENT_PLAN, "我先帮你查看当前志愿方案。");
        }
        return null;
    }

    private boolean hasPendingDeleteConfirmation(List<AgentMessage> recentMessages, int selectionIndex) {
        if (recentMessages == null || recentMessages.isEmpty()) {
            return false;
        }

        int assistantPromptIndex = -1;
        for (int i = recentMessages.size() - 1; i >= 0; i--) {
            AgentMessage message = recentMessages.get(i);
            if (!AgentRoles.ASSISTANT.equals(message.getRole())
                    || !AgentMessageTypes.TEXT.equals(message.getMessageType())) {
                continue;
            }
            String content = safeContent(message);
            if (content.contains("确认删除第" + selectionIndex + "个")) {
                assistantPromptIndex = i;
                break;
            }
            return false;
        }

        if (assistantPromptIndex < 1) {
            return false;
        }

        AgentMessage previousUserMessage = recentMessages.get(assistantPromptIndex - 1);
        if (!AgentRoles.USER.equals(previousUserMessage.getRole())) {
            return false;
        }
        String previousContent = safeContent(previousUserMessage);
        return containsAny(previousContent, "删除", "移除") && containsAny(previousContent, "志愿", "方案");
    }

    private String buildSystemPrompt() {
        return """
                你是高考志愿助手的受控编排器。你只能做十种决策：
                1. 调用 getUserProfile
                2. 调用 getCurrentPlan
                3. 调用 getSchoolDetail
                4. 调用 getSchoolDetailByName
                5. 调用 recommendSchools
                6. 调用 recommendMajors
                7. 调用 addPlanItem
                8. 调用 removePlanItem
                9. 调用 savePlan
                10. 直接回复

                你必须只输出 JSON：
                {
                  "action": "getUserProfile | getCurrentPlan | getSchoolDetail | getSchoolDetailByName | recommendSchools | recommendMajors | addPlanItem | removePlanItem | savePlan | reply",
                  "reply": "给用户的简短说明",
                  "toolArgs": {
                    "selectionIndex": "getSchoolDetail/addPlanItem/removePlanItem 时可选，默认 1",
                    "universityName": "getSchoolDetailByName 时必填",
                    "majorKeyword": "recommendMajors 时必填",
                    "planName": "savePlan 时必填"
                  }
                }

                对删除类操作，如果用户没有明确确认，不要调用 removePlanItem，只返回 reply 让用户确认。

                关键路由约束（必须严格遵守）：
                1. 推荐请求必须走工具：用户说"推荐学校""推荐专业""我想报XX""我想学XX"时，必须返回 recommendSchools 或 recommendMajors，不能直接 reply 道歉。
                2. 区分"生成方案"vs"查看方案"："生成/做/来个方案"→ recommendSchools；"查看/看看当前方案"→ getCurrentPlan。
                3. 用户描述自己分数/省份/科类时不应触发 getUserProfile：除非用户明确问"我的画像是什么""我的分数记录"。
                4. "看看XX大学"在没明确详情请求时不应该调 getSchoolDetailByName："看看能不能上XX"是推荐意图，应走 recommendSchools。

                不要输出任何额外文本。
                可用工具：
                %s
                """.formatted(agentToolRegistry.getToolDescriptions().entrySet().stream()
                .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\n")));
    }

    private String buildUserPrompt(String userMessage, List<AgentMessage> recentMessages, UserAccount user) {
        String history = recentMessages.stream()
                .map(message -> {
                    String payloadText = "";
                    if (message.getPayloadJson() != null && !message.getPayloadJson().isBlank()) {
                        payloadText = " | payload=" + message.getPayloadJson();
                    }
                    return "%s[%s]: %s%s".formatted(
                            message.getRole(),
                            message.getMessageType(),
                            message.getContent(),
                            payloadText
                    );
                })
                .collect(Collectors.joining("\n"));
        Map<String, Object> profile = new LinkedHashMap<>();
        profile.put("userId", user == null ? null : user.getId());
        profile.put("username", user == null ? null : user.getUsername());
        profile.put("score", user == null ? null : user.getScore());
        profile.put("subjectType", user == null || user.getSubjectType() == null ? null : user.getSubjectType().name());
        profile.put("examProvince", user == null ? null : user.getExamProvince());
        return "用户画像: " + profile
                + "\n最近消息:\n" + history
                + "\n当前用户消息:\n" + userMessage;
    }

    private Map<String, Object> readToolArgs(JsonNode toolArgsNode) {
        if (toolArgsNode == null || toolArgsNode.isMissingNode() || toolArgsNode.isNull() || !toolArgsNode.isObject()) {
            return Collections.emptyMap();
        }
        return objectMapper.convertValue(
                toolArgsNode,
                objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class)
        );
    }

    private String extractMajorKeyword(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        // 先精确匹配常见专业关键词：避免"推荐好的计算机专业"被正则捕获成"好的计算机"
        for (String keyword : MAJOR_KEYWORDS) {
            if (text.contains(keyword)) {
                return keyword;
            }
        }
        // 再走正则提取列表未覆盖的专业名，并清洗形容词等修饰词
        Matcher afterMatcher = RECOMMEND_MAJOR_AFTER_PATTERN.matcher(text);
        if (afterMatcher.find()) {
            String major = cleanMajorKeyword(afterMatcher.group(1));
            if (major != null) {
                return major;
            }
        }
        Matcher beforeMatcher = RECOMMEND_MAJOR_BEFORE_PATTERN.matcher(text);
        if (beforeMatcher.find()) {
            String major = cleanMajorKeyword(beforeMatcher.group(1));
            if (major != null) {
                return major;
            }
        }
        return null;
    }

    /** 去掉专业名前的形容词/修饰词，如"推荐好的计算机专业"→"计算机"。 */
    private String cleanMajorKeyword(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String cleaned = value.trim();
        for (String prefix : List.of("好的", "一些", "几个", "合适的", "优秀的", "较好的", "不错的", "好点的")) {
            if (cleaned.startsWith(prefix) && cleaned.length() > prefix.length()) {
                cleaned = cleaned.substring(prefix.length()).trim();
                break;
            }
        }
        return cleaned.isBlank() ? null : cleaned;
    }

    private int extractSelectionIndex(String text) {
        if (text == null || text.isBlank()) {
            return 1;
        }
        Matcher matcher = DIGIT_SELECTION_PATTERN.matcher(text);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        if (text.contains("第二")) {
            return 2;
        }
        if (text.contains("第三")) {
            return 3;
        }
        if (text.contains("第四")) {
            return 4;
        }
        if (text.contains("第五")) {
            return 5;
        }
        if (text.contains("第六")) {
            return 6;
        }
        return 1;
    }

    private String extractPlanName(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = SAVE_NAME_PATTERN.matcher(text);
        if (matcher.find()) {
            String name = matcher.group(1).trim();
            if (!name.isBlank() && !name.equals("方案")) {
                return name;
            }
        }
        return null;
    }

    private String extractSchoolName(String text) {
        if (text == null || text.isBlank()) {
            return null;
        }
        Matcher matcher = SCHOOL_NAME_DETAIL_PATTERN.matcher(text);
        if (matcher.find()) {
            String matched = matcher.group(1).trim();
            for (String prefix : List.of("帮我看看", "帮我查看", "帮我查查", "看看", "查看", "查查", "介绍一下")) {
                if (matched.startsWith(prefix)) {
                    matched = matched.substring(prefix.length()).trim();
                }
            }
            return matched.isBlank() ? null : matched;
        }
        return null;
    }

    private boolean containsOrdinalReference(String text) {
        return text.contains("第一个")
                || text.contains("第二个")
                || text.contains("第三个")
                || text.contains("第四个")
                || text.contains("第五个")
                || text.contains("第六个")
                || DIGIT_SELECTION_PATTERN.matcher(text).find();
    }

    private boolean containsAny(String text, String... keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }

    private String safeContent(AgentMessage message) {
        return message.getContent() == null ? "" : message.getContent();
    }
}
