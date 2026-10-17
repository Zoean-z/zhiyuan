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
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AgentDecisionService {

    private static final Logger log = LoggerFactory.getLogger(AgentDecisionService.class);

    /** 意图词表唯一运行时来源（阶段①：同义词外置 JSON）。 */
    private final AgentIntentLexicon lexicon;
    /** 阶段③：正则与词典都提取失败时，查专业目录兜底（唯一最长命中才采用）。 */
    private final AgentMajorCatalogService majorCatalogService;

    private final AiChatClient aiChatClient;
    private final ObjectMapper objectMapper;
    private final AgentToolRegistry agentToolRegistry;
    private final boolean qwenEnabled;
    private final boolean nativeToolCallingEnabled;
    /** 校准语义路由（Jev 原理，默认关闭）：disabled() 实例等价于"本层不存在"。 */
    private final SemanticRouterService semanticRouter;

    /**
     * 生产装配：qwenEnabled 控制 LLM 兜底层总开关；nativeToolCallingEnabled 决定
     * LLM 决策走原生 function-calling 还是旧的"提示词约束输出 JSON"。
     */
    @Autowired
    public AgentDecisionService(AiChatClient aiChatClient,
                                ObjectMapper objectMapper,
                                AgentToolRegistry agentToolRegistry,
                                AgentIntentLexicon intentLexicon,
                                AgentMajorCatalogService majorCatalogService,
                                SemanticRouterService semanticRouter,
                                @Value("${ai.qwen.enabled:true}") boolean qwenEnabled,
                                @Value("${ai.agent.tool-calling.enabled:true}") boolean nativeToolCallingEnabled) {
        this.aiChatClient = aiChatClient;
        this.objectMapper = objectMapper;
        this.agentToolRegistry = agentToolRegistry;
        this.lexicon = intentLexicon;
        this.majorCatalogService = majorCatalogService;
        this.semanticRouter = semanticRouter;
        this.qwenEnabled = qwenEnabled;
        this.nativeToolCallingEnabled = nativeToolCallingEnabled;
    }

    /**
     * 兼容既有测试装配：qwenEnabled=false 时 LLM 路径不可达，原生开关不起作用；
     * 目录服务以空 mapper 构建（惰性加载失败即降级为空目录），语料测试行为与改造前一致；
     * 校准路由以 disabled 实例注入（等价于旧版没有该层）。
     */
    AgentDecisionService(AiChatClient aiChatClient,
                         ObjectMapper objectMapper,
                         AgentToolRegistry agentToolRegistry,
                         boolean qwenEnabled) {
        this(aiChatClient, objectMapper, agentToolRegistry,
                new AgentIntentLexicon(objectMapper), new AgentMajorCatalogService(null),
                SemanticRouterService.disabled(), qwenEnabled, true);
    }

    public AgentDecision decide(String userMessage, List<AgentMessage> recentMessages, UserAccount user) {
        // 阶段②：单次状态构建（消息级派生 + 一遍窗口扫描），本地路由、守护闸门、
        // LLM 提示词三处消费同一份派生事实。
        AgentDialogState state = AgentDialogState.build(userMessage, recentMessages, user, lexicon, objectMapper);
        if (!semanticRouter.shadowEnabled()) {
            return decideCore(state, user);
        }
        // 影子模式（探针验证法）：主流程一字不变，仅旁路记录"校准路由会给什么"用于对账。
        AgentDecision decision = decideCore(state, user);
        semanticRouter.recordShadowComparison(state, buildUserPrompt(state), decision);
        return decision;
    }

    private AgentDecision decideCore(AgentDialogState state, UserAccount user) {
        AgentDecision localDecision = decideLocally(state);
        // Strong-intent keywords (recommend / profile / plan / delete / save / school
        // detail) are resolved locally with high precision. Let them short-circuit so
        // the tool actually runs instead of being bypassed by an LLM that prefers to
        // reply directly. Only when the local planner has no match do we ask the LLM
        // for semantic understanding of fuzzier requests.
        if (localDecision != null) {
            return localDecision;
        }
        // 校准路由（Jev 原理，默认关闭）：仅当高置信选中白名单业务工具才短路；
        // 无意见/低置信/超时/异常一律原样回落到下方既有 LLM 路径——最坏情况等于没有这一层。
        if (semanticRouter.routingActive()) {
            AgentDecision routed = semanticRouter.decideIfConfident(state, buildUserPrompt(state));
            if (routed != null) {
                return guardDestructiveCalls(routed, state);
            }
        }
        if (!qwenEnabled) {
            return defaultReply();
        }
        if (nativeToolCallingEnabled) {
            try {
                AgentDecision nativeDecision = decideWithNativeTools(state, user);
                return guardDestructiveCalls(nativeDecision, state);
            } catch (Exception ex) {
                // 原生通道失败（网络、上游不支持 tools、非 OpenAI 兼容网关）→ 降级回 JSON 模式
                log.warn("Agent native tool-calling decision failed, fallback to JSON mode: {}", ex.getMessage());
            }
        }
        try {
            String aiContent = aiChatClient.chat(
                    buildSystemPrompt(),
                    buildUserPrompt(state),
                    0.1,
                    true
            );
            JsonNode root = objectMapper.readTree(stripCodeFence(aiContent));
            String action = root.path("action").asText("").trim();
            String reply = root.path("reply").asText("").trim();
            Map<String, Object> toolArgs = readToolArgs(root.path("toolArgs"));
            // 模型缺 action 但 reply 有内容（澄清反问）时按直接回复处理，不丢弃有效回复
            if (action.isBlank() || AgentToolNames.REPLY.equals(action)) {
                return new AgentDecision(AgentToolNames.REPLY, reply.isBlank() ? DEFAULT_REPLY_TEXT : reply);
            }
            if (agentToolRegistry.supports(action)) {
                return guardDestructiveCalls(new AgentDecision(action, reply, toolArgs), state);
            }
        } catch (Exception ex) {
            log.warn("Agent AI decision failed, fallback to local planner: {}", ex.getMessage());
        }
        return defaultReply();
    }

    private static final String DEFAULT_REPLY_TEXT =
            "这句话我还没理解。你可以试试：“帮我推荐学校”、“看看我的画像”、“计算机专业学什么”、“把第 1 所加入志愿单”，或直接描述你的需求，我会调用对应工具完成。";

    private AgentDecision defaultReply() {
        return new AgentDecision(AgentToolNames.REPLY, DEFAULT_REPLY_TEXT);
    }

    /**
     * 模糊语义兜底的原生 tool-calling 路径：工具规格由 AgentToolRegistry 转成
     * OpenAI function-calling 声明直连 DeepSeek 的 tools 参数，模型原生选择工具与参数
     * （替代旧的"提示词约束输出 JSON"）；实时状态快照与历史压缩仍由 buildUserPrompt 注入。
     */
    private AgentDecision decideWithNativeTools(AgentDialogState state, UserAccount user) {
        AiChatClient.NativeToolCall call = aiChatClient.chatWithTools(
                buildNativeToolSystemPrompt(),
                buildUserPrompt(state),
                0.1,
                agentToolRegistry.getOpenAiToolDefinitions());
        String content = sanitizeReplyText(call.content() == null ? "" : call.content().trim());
        if (!call.isToolCall()) {
            // 模型未选任何工具（含 reply）：按直接回复处理
            return new AgentDecision(AgentToolNames.REPLY, content.isBlank() ? DEFAULT_REPLY_TEXT : content);
        }
        String action = call.toolName().trim();
        Map<String, Object> toolArgs = parseArgumentsJson(call.argumentsJson());
        if (AgentToolNames.REPLY.equals(action)) {
            String reply = toolArgs.get("reply") == null ? "" : sanitizeReplyText(String.valueOf(toolArgs.get("reply")).trim());
            if (reply.isBlank()) {
                // 部分模型把回复放进 content 而非参数，兼容取用
                reply = content;
            }
            return new AgentDecision(AgentToolNames.REPLY, reply.isBlank() ? DEFAULT_REPLY_TEXT : reply);
        }
        if (!agentToolRegistry.supports(action)) {
            log.warn("Agent LLM selected unknown tool '{}', fallback to default reply", action);
            return defaultReply();
        }
        // 工具调用的一句说明放 content；模型未给时用与本地规划器同款的确定性话术兜底
        String reply = content.isBlank() ? AgentRoutingPreambles.forTool(action, toolArgs) : content;
        return new AgentDecision(action, reply, toolArgs);
    }

    /** 模型偶发用 ```json 围栏包裹输出：剥掉围栏再解析（剥后仍非法则走既有降级路径）。 */
    private String stripCodeFence(String content) {
        if (content == null) {
            return null;
        }
        String text = content.trim();
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline > 0 && lastFence > firstNewline) {
                text = text.substring(firstNewline + 1, lastFence).trim();
            }
        }
        return text;
    }

    /**
     * 清理 LLM 输出的包装残留：部分模型会把回复文本包上字面 {@code <reply>…</reply>}
     * 标签（模仿工具名），或用 markdown 代码栅栏包裹——原样透传会被用户看到。
     */
    private String sanitizeReplyText(String text) {
        if (text == null) {
            return "";
        }
        String cleaned = text.trim();
        cleaned = cleaned.replaceFirst("^(<reply>|```reply\\s*|```json\\s*)", "")
                         .replaceFirst("(</reply>|```)$", "")
                         .trim();
        return cleaned;
    }

    private Map<String, Object> parseArgumentsJson(String argumentsJson) {        if (argumentsJson == null || argumentsJson.isBlank()) {
            return Collections.emptyMap();
        }
        try {
            JsonNode node = objectMapper.readTree(argumentsJson);
            if (!node.isObject()) {
                return Collections.emptyMap();
            }
            return objectMapper.convertValue(node,
                    objectMapper.getTypeFactory().constructMapType(LinkedHashMap.class, String.class, Object.class));
        } catch (Exception ex) {
            log.warn("Agent LLM tool arguments are not valid JSON: {}", argumentsJson);
            return Collections.emptyMap();
        }
    }

    /**
     * 删除是破坏性操作：无论模型多确信，没有「用户请求 → 助手确认提示」的两段上下文就不放行。
     * 这是提示词约束之外的代码级闸门，对原生 tool-calling 与 JSON 模式同样生效——模糊语义
     * 覆盖面扩大后，未确认的删除绝不能因模型自信而执行。
     */
    private AgentDecision guardDestructiveCalls(AgentDecision decision, AgentDialogState state) {
        if (!AgentToolNames.REMOVE_PLAN_ITEM.equals(decision.getAction())) {
            return decision;
        }
        Object raw = decision.getToolArgs().get("selectionIndex");
        // L-20260921 扫描：序号缺失时不得兜底编造"第 1 个"——那会引导用户确认删除一个
        // 与其意图无关的条目。改为请用户明确序号（fail-closed）。
        if (raw == null) {
            return new AgentDecision(
                    AgentToolNames.REPLY,
                    "请告诉我要删除志愿表中的第几项（例如：删除第 1 个），我会先和你确认。"
            );
        }
        int selectionIndex;
        if (raw instanceof Number number) {
            selectionIndex = number.intValue();
        } else {
            try {
                selectionIndex = Integer.parseInt(String.valueOf(raw).trim());
            } catch (Exception ignored) {
                // 序号无法解析（如"abc"）：与缺失同样请用户指明，不编造"第 1 个"
                return new AgentDecision(
                        AgentToolNames.REPLY,
                        "请告诉我要删除志愿表中的第几项（例如：删除第 1 个），我会先和你确认。"
                );
            }
        }
        if (state.hasPendingDeleteConfirmation(selectionIndex)) {
            return decision;
        }
        // Noul 语义确认（默认关闭）：只有 provider 以高置信明确确认才放行转述式删除；
        // 无意见/低置信/异常一律 fail-closed，与旧路径一样要求用户显式确认。
        if (semanticRouter.deleteConfirmEnabled()
                && semanticRouter.deletionConfirmedBySemantics(state, selectionIndex)) {
            return decision;
        }
        return new AgentDecision(
                AgentToolNames.REPLY,
                "删除是敏感操作。若确认删除当前志愿单中的第 %s 个结果，请回复\u201c确认删除第%s个\u201d。"
                        .formatted(selectionIndex, selectionIndex)
        );
    }

    /**
     * 原生 tool-calling 模式的系统提示词：工具清单与参数 Schema 由 API 的 tools 声明承载，
     * 提示词只保留角色设定与路由约束（与 buildSystemPrompt 的 JSON 模式文档解耦）。
     */
    private String buildNativeToolSystemPrompt() {
        return """
                你是高考志愿助手的受控编排器。通过选择并调用工具完成用户请求；闲聊、澄清追问、引导补充信息或没有任何工具匹配时，调用 reply 工具直接回复。

                调用业务工具时，在同一条消息的 content 里用一句话向用户说明你将做什么；调用 reply 时把给用户的完整回复放进 reply 参数。

                关键路由约束（必须严格遵守）：
                1. 推荐请求必须走工具：用户说"推荐学校""推荐专业""我想报XX""我想学XX"时，必须调用 recommendSchools 或 recommendMajors，不能只 reply 道歉。
                2. 区分"生成方案"与"查看方案"："生成/做/来个方案"→ recommendSchools；"查看/看看当前方案"→ getCurrentPlan。
                3. 用户描述自己的分数/省份/科类时不要调用 getUserProfile：除非用户明确在问"我的画像是什么""我的分数记录"。
                4. "看看XX大学"在没明确详情请求时不要调 getSchoolDetailByName："看看能不能上XX"是推荐意图，应走 recommendSchools。
                5. "XX专业怎么样"、"就业前景"、"学什么"、"课程介绍"等专业知识问题必须调用 getMajorOverview；只有明确要求"推荐专业/适合报什么专业"时才调用 recommendMajors。
                6. 用户只要求查看画像时只调用 getUserProfile，不要额外调用推荐工具。
                7. 结合"系统实时状态"选择工具：最近推荐不可用或为 0 项时，不要调用 addPlanItem/removePlanItem（应先 recommendSchools）；引用"第 N 个"时 N 不得超过最近推荐的项数。
                8. removePlanItem 必须先经用户确认：没有明确的确认语境时，只能调用 reply 引导用户回复"确认删除第N个"。
                """;
    }

    private String buildSystemPrompt() {
        String tools = agentToolRegistry.getToolDescriptions().entrySet().stream()
                .map(entry -> "- " + entry.getKey() + ": " + entry.getValue())
                .collect(Collectors.joining("\\n"));
        String actionEnum = agentToolRegistry.listSpecs().stream()
                .map(AgentToolSpec::name)
                .collect(Collectors.joining(" | ")) + " | reply";
        return """
                你是高考志愿助手的受控编排器。你只能做十一种决策：
                1. 调用 getUserProfile
                2. 调用 getCurrentPlan
                3. 调用 getSchoolDetail
                4. 调用 getSchoolDetailByName
                5. 调用 getMajorOverview
                6. 调用 recommendSchools
                7. 调用 recommendMajors
                8. 调用 addPlanItem
                9. 调用 removePlanItem
                10. 调用 savePlan
                11. 直接回复

                你必须只输出 JSON：
                {
                  "action": "%s",
                  "reply": "给用户的简短说明",
                  "toolArgs": { 按下方工具参数说明提供 }
                }

                对删除类操作，如果用户没有明确确认，不要调用 removePlanItem，只返回 reply 让用户确认。

                关键路由约束（必须严格遵守）：
                1. 推荐请求必须走工具：用户说"推荐学校""推荐专业""我想报XX""我想学XX"时，必须返回 recommendSchools 或 recommendMajors，不能直接 reply 道歉。
                2. 区分"生成方案"vs"查看方案"："生成/做/来个方案"→ recommendSchools；"查看/看看当前方案"→ getCurrentPlan。
                3. 用户描述自己分数/省份/科类时不应触发 getUserProfile：除非用户明确问"我的画像是什么""我的分数记录"。
                4. "看看XX大学"在没明确详情请求时不应该调 getSchoolDetailByName："看看能不能上XX"是推荐意图，应走 recommendSchools。
                5. "XX专业怎么样"、"就业前景"、"学什么"、"课程介绍"等专业知识问题必须调用 getMajorOverview；只有明确要求“推荐专业/适合报什么专业”时才调用 recommendMajors。
                6. 用户只要求查看画像时只调用 getUserProfile，不要额外调用推荐工具。
                7. 结合"系统实时状态"选择工具：最近推荐不可用或为 0 项时，不要调用 addPlanItem/removePlanItem（应先 recommendSchools）；引用"第 N 个"时 N 不得超过最近推荐的项数。

                不要输出任何额外文本。
                可用工具与参数 Schema（由注册表自动生成）：
                %s
                """.formatted(actionEnum, agentToolRegistry.getToolSchemaMarkdown() + tools);
    }

    /**
     * 上下文装配（context engineering，参考 Claude Code / ZCode 的上下文管理纪律）：
     * <ul>
     *   <li>大载荷出带：工具结果 JSON（如 55 项推荐载荷）永不进入提示词正文，
     *       只保留摘要与条数指针——执行层从消息库按需取用完整数据；</li>
     *   <li>两级历史压缩：窗口内较早的消息压成单行摘要，最近若干条保留原文（仍截断），
     *       同样的 token 预算容纳更多轮次；</li>
     *   <li>系统实时状态快照：由 AgentDialogState 单次构建提供。</li>
     * </ul>
     */
    String buildUserPrompt(AgentDialogState state) {
        List<AgentMessage> recentMessages = state.recentMessages();
        int total = recentMessages.size();
        int digestCount = Math.max(0, total - VERBATIM_HISTORY_MESSAGES);
        StringBuilder history = new StringBuilder();
        if (digestCount > 0) {
            history.append("（更早 ").append(digestCount).append(" 条消息，摘要）\n");
            for (AgentMessage message : recentMessages.subList(0, digestCount)) {
                history.append(renderDigest(message)).append('\n');
            }
            history.append("—— 以上为摘要，以下为最近消息原文 ——\n");
        }
        for (AgentMessage message : recentMessages.subList(digestCount, total)) {
            history.append(renderRecent(message)).append('\n');
        }
        return "用户画像: " + state.profileMap()
                + "\n系统实时状态:\n" + state.snapshotText()
                + "\n最近消息:\n" + history
                + "\n当前用户消息:\n" + state.normalizedMessage();
    }

    /** 兼容入口：按消息列表直接构建状态并渲染（上下文装配契约测试走这里）。 */
    String buildUserPrompt(String userMessage, List<AgentMessage> recentMessages, UserAccount user) {
        return buildUserPrompt(AgentDialogState.build(userMessage, recentMessages, user, lexicon, objectMapper));
    }

    /** 常量：窗口内保留原文的最近消息条数；更早的消息压成摘要。 */
    private static final int VERBATIM_HISTORY_MESSAGES = 6;

    /** 较早消息的单行摘要（脱载荷）。 */
    private String renderDigest(AgentMessage message) {
        String extra = "";
        if (AgentMessageTypes.TOOL_RESULT.equals(message.getMessageType())
                && (AgentToolNames.RECOMMEND_SCHOOLS.equals(message.getToolName())
                    || AgentToolNames.RECOMMEND_MAJORS.equals(message.getToolName()))
                && message.getPayloadJson() != null && !message.getPayloadJson().isBlank()) {
            extra = " | " + recommendPayloadDigest(message.getPayloadJson());
        }
        return "- [%s/%s] %s%s".formatted(
                message.getRole(), message.getMessageType(),
                truncate(safeContent(message), 80), extra);
    }

    /** 最近消息的原文渲染：内容截断，载荷只保留摘要指针，完整数据留在消息库。 */
    private String renderRecent(AgentMessage message) {
        String extra = "";
        if (AgentMessageTypes.TOOL_RESULT.equals(message.getMessageType())
                && message.getPayloadJson() != null && !message.getPayloadJson().isBlank()) {
            extra = " | " + recommendPayloadDigest(message.getPayloadJson());
        }
        return "%s[%s]: %s%s".formatted(
                message.getRole(), message.getMessageType(),
                truncate(safeContent(message), AgentDialogState.MAX_RENDERED_CONTENT_CHARS), extra);
    }

    /** 从推荐载荷提取"条数 + 前几项名称"的摘要。 */
    private String recommendPayloadDigest(String payloadJson) {
        try {
            JsonNode topItems = objectMapper.readTree(payloadJson).path("topItems");
            if (!topItems.isArray()) {
                return "载荷存在";
            }
            int total = topItems.size();
            StringBuilder labels = new StringBuilder();
            for (int i = 0; i < Math.min(3, total); i++) {
                String label = topItems.get(i).path("label").asText("");
                if (!label.isBlank()) {
                    labels.append(labels.length() == 0 ? "" : "、").append(label);
                }
            }
            return "推荐载荷共 %d 项（前几项：%s；完整数据由执行层按需取用）".formatted(total, labels);
        } catch (Exception ex) {
            return "载荷存在";
        }
    }

    private String truncate(String text, int max) {
        if (text == null) {
            return "";
        }
        return text.length() <= max ? text : text.substring(0, max) + "…";
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

    private String safeContent(AgentMessage message) {
        return message.getContent() == null ? "" : message.getContent();
    }

    // ------------------------------------------------------------------
    // 本地强意图规划器：只保留组合与排除的布尔结构，词表见 agent/intent-keywords.json
    // ------------------------------------------------------------------

    private AgentDecision decideLocally(AgentDialogState state) {
        String normalized = state.normalizedMessage();

        // --- #1: removePlanItem 确认删除 ---
        // 否定守卫（L-20260921 扫描）："先不确认删除/不删了"包含"确认删除"子串，
        // 纯 contains 会误执行破坏性操作，必须先排除否定语境。
        if (lexicon.containsAny(normalized, "deleteConfirm")
                && !lexicon.containsAny(normalized, "deleteConfirmNegations")) {
            int selectionIndex = state.selectionIndex();
            if (state.hasPendingDeleteConfirmation(selectionIndex)) {
                return new AgentDecision(
                        AgentToolNames.REMOVE_PLAN_ITEM,
                        "我现在删除当前志愿单中的第 %s 个结果。".formatted(selectionIndex),
                        Map.of("selectionIndex", selectionIndex)
                );
            }
            return new AgentDecision(AgentToolNames.REPLY, "我没有检测到最近一条待确认的删除请求，请先明确告诉我要删除哪一项，再按提示确认。");
        }

        // --- P1 #2: 删除提示路由收紧 ---
        // 要求 "删除/移除" + "志愿/方案" + ("当前" 或 序号引用)；排除过去时陈述
        if (lexicon.containsAny(normalized, "deleteVerbs")
                && lexicon.containsAny(normalized, "planNouns")
                && (lexicon.containsAny(normalized, "currentReference") || state.ordinalReference())
                && !lexicon.containsAny(normalized, "deletePastTense")) {
            int selectionIndex = state.selectionIndex();
            return new AgentDecision(
                    AgentToolNames.REPLY,
                    "删除是敏感操作。若确认删除当前志愿单中的第 %s 个结果，请回复\u201c确认删除第%s个\u201d。".formatted(selectionIndex, selectionIndex)
            );
        }

        // --- #3: savePlan ---
        if (lexicon.containsAny(normalized, "savePlanTriggers")) {
            String planName = state.planName();
            if (planName == null || planName.isBlank()) {
                return new AgentDecision(AgentToolNames.REPLY, "请直接告诉我方案名，例如：保存为\u201c冲稳保方案\u201d。");
            }
            return new AgentDecision(
                    AgentToolNames.SAVE_PLAN,
                    "我现在把当前志愿单保存为《%s》。".formatted(planName),
                    Map.of("planName", planName)
            );
        }

        // --- #4: addPlanItem ---
        if (lexicon.containsAny(normalized, "addPlanItemTriggers")) {
            int selectionIndex = state.selectionIndex();
            Map<String, Object> args = new LinkedHashMap<>();
            args.put("selectionIndex", selectionIndex);
            // L-20260921 扫描：用户点名的学校优先于序号——"把中南大学加入志愿单"
            // 不再默认加第 1 项，由执行层在最近推荐载荷里按名匹配
            String namedSchool = state.schoolName() != null ? state.schoolName() : state.lastMentionedSchool();
            if (namedSchool != null && !namedSchool.isBlank()) {
                args.put("schoolName", namedSchool);
            }
            return new AgentDecision(
                    AgentToolNames.ADD_PLAN_ITEM,
                    args.containsKey("schoolName")
                            ? "我先把最近推荐里的「%s」加入当前志愿单。".formatted(namedSchool)
                            : "我先把最近推荐里的第 %s 个结果加入当前志愿单。".formatted(selectionIndex),
                    args
            );
        }

        // --- 专业介绍：必须先于学校详情与推荐处理（L-20260921-08 答非所问）---
        // "它的电子信息工程专业怎么样"这类带着具体专业名+问询语气的消息，应答专业本身，
        // 而不是被下方的"对话续槽/校名优先"分支抢先倒出整张学校专业表。
        String majorKeyword = state.majorKeyword();
        if (majorKeyword == null && state.overviewCue()) {
            majorKeyword = state.majorOverviewKeyword();
        }
        // 阶段③：正则与词典都提取失败时，查专业目录兜底（唯一最长命中才采用）。
        // 仅在专业问询/推荐语境下扫描，避免闲聊消息触发无谓匹配。
        if (majorKeyword == null
                && (state.overviewCue()
                    || (lexicon.containsAny(normalized, "recommendCue") && lexicon.containsAny(normalized, "majorDirectionNouns")))) {
            majorKeyword = majorCatalogService.findInText(normalized).orElse(null);
        }
        if (isMajorOverviewRequest(normalized, majorKeyword)) {
            return new AgentDecision(
                    AgentToolNames.GET_MAJOR_OVERVIEW,
                    "我先查询“%s”的学习内容、就业方向和报考提醒。".formatted(majorKeyword),
                    Map.of("majorKeyword", majorKeyword)
            );
        }

        // --- P0 #5: getSchoolDetailByName 收紧 ---
        // 要求 "查看/看看" + 校名 + "详情/信息/专业" 三者同时出现
        String schoolName = state.schoolName();
        if (!state.ordinalReference()
                && schoolName != null
                && lexicon.containsAny(normalized, "detailViewVerbs")
                && !lexicon.containsAny(normalized, "detailIntentExclusions")
                && (lexicon.containsAny(normalized, "detailNouns")
                    || lexicon.containsAny(normalized, "detailQueryVerbs"))) {
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME,
                    "我先按学校名帮你查询\u201c%s\u201d的详情和可参考专业。".formatted(schoolName),
                    Map.of("universityName", schoolName)
            );
        }

        // --- P0 #6: getSchoolDetail 收紧 ---
        // 要求 序号引用(第N个) + "详情/信息/专业" 组合，去掉泛词单独触发
        if (state.ordinalReference()
                && lexicon.containsAny(normalized, "schoolDetailNouns")) {
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL,
                    "我先帮你查看第 %s 个学校的详情和可参考专业。".formatted(state.selectionIndex()),
                    Map.of("selectionIndex", state.selectionIndex())
            );
        }

        // --- 对话续槽（学校上下文）：用"他/它/该校"指代上文学校并问专业/详情 ---
        String lastMentionedSchool = state.lastMentionedSchool();
        if (lastMentionedSchool != null
                && lexicon.containsAny(normalized, "schoolPronouns")
                && lexicon.containsAny(normalized, "detailNouns")
                && lexicon.containsAny(normalized, "pronounFollowIntents")) {
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME,
                    "好的，基于我们刚聊到的%s，我把它的专业信息整理如下：".formatted(lastMentionedSchool),
                    Map.of("universityName", lastMentionedSchool)
            );
        }

        // --- 校名优先：显式校名 + 专业/热门/推荐 → 该校详情（专业列表） ---
        String mentionedSchoolForMajor = state.strippedSchoolName();
        if (mentionedSchoolForMajor != null
                && !state.ordinalReference()
                && lexicon.containsAny(normalized, "schoolContextCues")) {
            return new AgentDecision(
                    AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME,
                    "好的，我把%s的专业信息整理如下，热门方向已标注：".formatted(mentionedSchoolForMajor),
                    Map.of("universityName", mentionedSchoolForMajor)
            );
        }

        // --- P1 #7: recommendMajors ---
        if (lexicon.containsAny(normalized, "recommendCue")
                && lexicon.containsAny(normalized, "majorDirectionNouns") && majorKeyword == null) {
            return new AgentDecision(AgentToolNames.REPLY,
                    "想看哪一类专业？告诉我方向（例如：计算机、电子信息、临床医学），我再基于你的画像生成专业推荐。");
        }
        if (lexicon.containsAny(normalized, "recommendCue") && majorKeyword != null) {
            return new AgentDecision(
                    AgentToolNames.RECOMMEND_MAJORS,
                    "我先基于你的画像和\u201c%s\u201d的兴趣给你生成专业推荐。".formatted(majorKeyword),
                    Map.of("majorKeyword", majorKeyword)
            );
        }

        // --- #8: recommendSchools ---
        if (lexicon.containsAny(normalized, "recommendSchoolsTriggers") ||
                (lexicon.containsAny(normalized, "chongWenBaoCue") && lexicon.containsAny(normalized, "chongWenBaoCombos"))) {
            return new AgentDecision(AgentToolNames.RECOMMEND_SCHOOLS, "我先基于你当前画像给你生成学校推荐。");
        }

        // --- P0 #9: getUserProfile 路由收紧 ---
        if (lexicon.containsAny(normalized, "profileQueries")
                && !lexicon.containsAny(normalized, "profileEditExclusions")) {
            return new AgentDecision(AgentToolNames.GET_USER_PROFILE, "我先帮你读取当前画像信息。");
        }

        // --- #10: getCurrentPlan ---
        if (lexicon.containsAny(normalized, "currentPlanTriggers") ||
                (lexicon.containsAny(normalized, "currentReference") && lexicon.containsAny(normalized, "planNouns"))) {
            return new AgentDecision(AgentToolNames.GET_CURRENT_PLAN, "我先帮你查看当前志愿方案。");
        }

        // --- 对话续槽：上一轮助手主动追问"专业方向"，本轮短回复即填槽答案 ---
        if (state.pendingMajorDirectionSlot()) {
            String direction = state.majorKeyword();
            if (direction != null) {
                return new AgentDecision(
                        AgentToolNames.RECOMMEND_MAJORS,
                        "好的，按“%s”方向基于你的画像生成专业推荐。".formatted(direction),
                        Map.of("majorKeyword", direction)
                );
            }
            if (lexicon.containsAny(normalized, "slotUnknownReplies")) {
                return new AgentDecision(AgentToolNames.REPLY, "那不如先看学校推荐？回复“帮我推荐学校”即可。");
            }
            return new AgentDecision(AgentToolNames.REPLY,
                    "没听清专业方向。请回复一个方向，例如：计算机、电子信息、临床医学。");
        }
        return null;
    }

    private boolean isMajorOverviewRequest(String text, String majorKeyword) {
        if (majorKeyword == null || majorKeyword.isBlank() || !lexicon.containsAny(text, "majorOverviewCues")) {
            return false;
        }
        // A request for matching, admission probability, or a school list still belongs to
        // recommendation/probability workflows even if it mentions a major name.
        return !lexicon.containsAny(text, "majorOverviewExclusions");
    }
}
