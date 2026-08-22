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
                    你是高考志愿填报顾问。当前推荐系统因数据库缺少该省份/科类的录取线数据，未能给出精确院校推荐。
                    请基于考生画像和意图，给出方向性建议，覆盖：
                    1. 该分数段大致可考虑的院校层次（如省重点、普通一本、二本头部等，不要点名具体院校）
                    2. 地域选择策略（本省 vs 外省、城市层级取舍）
                    3. 专业方向建议（若用户指定了专业关键词，谈该专业就业/深造方向；否则给通用的冲稳保搭配思路）
                    4. 风险提示（滑档/退档、招生章程核对、近三年位次趋势）

                    硬性要求：
                    - 不要编造具体院校名或分数线数字
                    - 控制在 200 字以内
                    - 直接输出建议正文，不要前后寒暄
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
