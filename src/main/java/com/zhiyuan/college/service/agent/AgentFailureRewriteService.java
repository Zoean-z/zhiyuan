package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.service.AiChatClient;
import java.util.Set;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * 工具失败 → LLM 改写兜底（产品指定的对话设计）：
 * 系统内置工具无法命中用户指令（学校未收录、数据缺失等硬失败）时，不再把红色报错
 * 直接抛给用户，而是把"用户原话 + 失败的工具与原因"交给 LLM，由它用自身的高考志愿
 * 通用知识给出有帮助的回复——即"把 LLM 的完整性知识搬到对话回复，但基于系统语境输出"。
 *
 * <p>安全边界（错误驱动，逐条对应新层可能引入的故障）：
 * <ul>
 *   <li>只改写<b>只读查询类</b>工具的失败；志愿单增/删/存类失败涉及用户数据状态，
 *       继续走确定性引导话术，LLM 不得自由发挥；</li>
 *   <li>提示词硬约束：禁止编造系统数据（录取分/位次/概率），通用知识必须显式标注
 *       "非本系统数据"；</li>
 *   <li>fail-open：LLM 异常/超时/空白回复一律返回 null，调用方回落旧失败话术——
 *       最坏情况等于没有本层；</li>
 *   <li>开关 {@code ai.agent.failure-rewrite.enabled}（默认开），可随时整体回退旧行为。</li>
 * </ul>
 */
@Service
public class AgentFailureRewriteService {

    private static final Logger log = LoggerFactory.getLogger(AgentFailureRewriteService.class);

    /** 允许改写的只读工具白名单（志愿单增/删/存不在其中）。 */
    private static final Set<String> REWRITABLE_TOOLS = Set.of(
            AgentToolNames.GET_USER_PROFILE,
            AgentToolNames.GET_CURRENT_PLAN,
            AgentToolNames.GET_SCHOOL_DETAIL,
            AgentToolNames.GET_SCHOOL_DETAIL_BY_NAME,
            AgentToolNames.GET_MAJOR_OVERVIEW,
            AgentToolNames.RECOMMEND_SCHOOLS,
            AgentToolNames.RECOMMEND_MAJORS
    );

    private final AiChatClient aiChatClient;
    private final boolean enabled;

    @Autowired
    public AgentFailureRewriteService(
            AiChatClient aiChatClient,
            @Value("${ai.agent.failure-rewrite.enabled:true}") boolean enabled) {
        this.aiChatClient = aiChatClient;
        this.enabled = enabled;
        if (!enabled) {
            log.info("Agent failure rewrite disabled (ai.agent.failure-rewrite.enabled=false)");
        }
    }

    public boolean isEnabled() {
        return enabled;
    }

    /** 该工具失败是否允许 LLM 改写（只读白名单 + 总开关）。 */
    public boolean isRewritable(String toolName) {
        return enabled && toolName != null && REWRITABLE_TOOLS.contains(toolName);
    }

    /**
     * 生成改写回复；任何失败返回 null（调用方回落旧失败话术）。
     *
     * @param userMessage  用户原话
     * @param toolName     失败的工具名
     * @param toolArgsJson 工具入参（诊断用）
     * @param errorMessage 工具失败原因（已是人话）
     */
    public String rewrite(String userMessage, String toolName, String toolArgsJson, String errorMessage) {
        if (!isRewritable(toolName)) {
            return null;
        }
        try {
            String reply = aiChatClient.chat(buildSystemPrompt(), buildUserPrompt(
                    userMessage, toolName, toolArgsJson, errorMessage), 0.3, false);
            if (reply == null || reply.isBlank()) {
                log.warn("Agent failure rewrite returned blank reply for tool {}", toolName);
                return null;
            }
            return reply.trim();
        } catch (Exception ex) {
            log.warn("Agent failure rewrite failed, fall back to legacy error text: {}", ex.getMessage());
            return null;
        }
    }

    private String buildSystemPrompt() {
        return """
                不要输出百分比录取概率或数字风险指数，使用定性机会等级。
                你是高考志愿助手"小智"。刚才系统内部查询工具调用失败了，现在需要你直接回复用户。

                回复规则（必须严格遵守）：
                1. 先理解用户想问什么，结合你的高考志愿通用知识尽量给出有用的回答；
                2. 通用知识部分必须明确标注"（以下为通用信息，非本系统数据）"；
                3. 绝对禁止编造具体学校的录取分数线、最低位次、录取概率等系统数据——不知道就说明
                   "该数据需以系统查询结果为准"；
                4. 失败原因若是"学校未收录"，提示用户换一所数据集内的学校或使用完整校名再试；
                5. 全文不超过 150 字，语气友好，不要出现"工具""报错""失败"等技术字眼。
                """;
    }

    private String buildUserPrompt(String userMessage, String toolName, String toolArgsJson, String errorMessage) {
        return "用户原话：" + (userMessage == null ? "" : userMessage)
                + "\n失败的工具：" + toolName
                + "\n工具入参：" + (toolArgsJson == null ? "{}" : toolArgsJson)
                + "\n失败原因：" + (errorMessage == null ? "未知" : errorMessage)
                + "\n请直接给出给用户的回复。";
    }
}
