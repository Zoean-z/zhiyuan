package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.model.dto.RecommendationRequest;
import com.zhiyuan.college.model.dto.RecommendationResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import com.zhiyuan.college.model.enums.RecommendationMode;
import com.zhiyuan.college.service.AiChatClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Directional advice produced by the LLM when the recommendation engine returns
 * no matching rows (a data gap in the admission_cutoff table for the user's
 * province/subject combination).
 *
 * <p>This is explicitly a fallback, not a substitute for real admission data:
 * the prompt forbids fabricating specific university names or cutoff scores, and
 * the output is prefixed with a non-authoritative disclaimer. Callers must set
 * {@code payload.fallback = true} so the frontend can render it differently from
 * a precise recommendation.
 */
@Service
public class AgentFallbackAdviceService {

    private static final Logger log = LoggerFactory.getLogger(AgentFallbackAdviceService.class);

    private static final String DISCLAIMER = "以下为方向性参考，非精确录取数据，具体以官方招生章程为准。";

    private final AiChatClient aiChatClient;

    public AgentFallbackAdviceService(AiChatClient aiChatClient) {
        this.aiChatClient = aiChatClient;
    }

    /**
     * Generate directional advice for a user whose recommendation came back empty.
     *
     * @return advice text (already prefixed with the disclaimer), or {@code null}
     *         if the LLM call fails so the caller can keep the original empty-result
     *         summary without blocking the conversation.
     */
    public String generateAdvice(UserAccount user, RecommendationRequest request, RecommendationResponse response) {
        if (user == null || request == null) {
            return null;
        }
        try {
            String systemPrompt = """
                    你是资深高考志愿填报顾问。当前推荐系统因数据库缺少该省份/科类的录取线数据，未能给出精确院校推荐，需要你基于通用高考常识给出结构化方向性建议。

                    输出格式（纯 markdown，不要代码块包裹）：

                    ## 一、分数段研判
                    结合该省近年物理类/历史类特殊类型控制线、一本线/本科线，分析该分数所处位置与竞争态势（约 80 字）。

                    ## 二、可关注院校方向
                    按"省外（冲/稳）"和"省内（保）"两组，每组点名 2-3 所该分数段典型可关注院校。每所院校格式：
                    - **院校名（参考，需核实）**：推荐专业方向、近年参考约 XXX-XXX 分（以官方为准）、核心优势一句话

                    ## 三、高就业专业推荐
                    结合考生科类，推荐 2-3 类就业导向专业方向，每类说明适配理由与就业去向。

                    ## 四、院校+专业匹配
                    点出 2-3 个"院校+专业"典型组合，说明匹配逻辑。

                    ## 五、填报策略
                    按"冲一冲/稳一稳/保一保"三档给框架，每档标注参考分数区间与侧重点。

                    ## 六、风险提醒
                    列 2-3 条（选科隐性要求、学费差异、调剂退档等）。

                    ## 七、总结
                    一段话总结核心建议。

                    硬性要求：
                    - 所有院校名后必须标"（参考，需核实）"
                    - 所有分数区间用"近年参考约 XXX-XXX 分（以官方为准）"，不要给精确到个位的录取线
                    - 不要编造绝对不存在的院校
                    - 直接输出 markdown 正文，不要寒暄
                    - 总字数 400-600 字
                    """;
            String userPrompt = buildUserPrompt(user, request);
            String advice = aiChatClient.chat(systemPrompt, userPrompt, 0.3, false);
            if (advice == null || advice.isBlank()) {
                return null;
            }
            return DISCLAIMER + advice.trim();
        } catch (Exception ex) {
            log.warn("Agent fallback advice generation failed: {}", ex.getMessage());
            return null;
        }
    }

    private String buildUserPrompt(UserAccount user, RecommendationRequest request) {
        StringBuilder sb = new StringBuilder();
        sb.append("考生画像：分数=").append(user.getScore());
        sb.append("，科类=").append(user.getSubjectType() == null ? "未知" : user.getSubjectType().name());
        sb.append("，省份=").append(user.getExamProvince() == null ? "未知" : user.getExamProvince());
        sb.append("。\n");
        sb.append("推荐意图：");
        sb.append(request.getRecommendationMode() == RecommendationMode.MAJOR_FIRST ? "专业优先" : "院校优先");
        if (request.getMajorKeyword() != null && !request.getMajorKeyword().isBlank()) {
            sb.append("，专业关键词=").append(request.getMajorKeyword());
        }
        sb.append("。\n");
        sb.append("系统状态：数据库未命中该省份/科类的精确录取线，请给方向性建议。");
        return sb.toString();
    }
}
