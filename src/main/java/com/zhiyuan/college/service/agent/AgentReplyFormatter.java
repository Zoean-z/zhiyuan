package com.zhiyuan.college.service.agent;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.entity.UserAccount;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

/**
 * Renders the agent's final reply as structured markdown instead of the bare
 * tool summary. For normal recommendations it builds the sections from real
 * recommendation data (no LLM call — fast, accurate, no hallucination). For
 * fallback (empty data) cases it returns the LLM-generated fallbackAdvice as-is.
 */
@Service
public class AgentReplyFormatter {

    private static final Logger log = LoggerFactory.getLogger(AgentReplyFormatter.class);

    private final ObjectMapper objectMapper;

    public AgentReplyFormatter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String format(AgentToolResult toolResult, UserAccount user) {
        if (toolResult == null) {
            return "";
        }
        JsonNode payload = parsePayload(toolResult.getPayloadJson());
        if (payload == null) {
            return toolResult.getSummary();
        }
        String overviewMarkdown = payload.path("overviewMarkdown").asText("").trim();
        if (!overviewMarkdown.isBlank()) {
            return overviewMarkdown;
        }
        // Fallback path: LLM already produced directional advice.
        if (payload.path("fallback").asBoolean(false)) {
            String advice = payload.path("fallbackAdvice").asText("");
            return advice.isBlank() ? toolResult.getSummary() : advice;
        }
        JsonNode topItems = payload.path("topItems");
        if (!topItems.isArray() || topItems.isEmpty()) {
            return toolResult.getSummary();
        }
        return renderRecommendationMarkdown(payload, topItems, user);
    }

    private String renderRecommendationMarkdown(JsonNode payload, JsonNode topItems, UserAccount user) {
        StringBuilder sb = new StringBuilder();
        int score = user != null && user.getScore() != null ? user.getScore() : 0;
        String subject = user != null && user.getSubjectType() != null ? user.getSubjectType().name() : "未知";
        String province = user != null && user.getExamProvince() != null ? user.getExamProvince() : "未知";
        JsonNode userRankNode = payload.path("userRank");
        String userRank = userRankNode.isMissingNode() || userRankNode.isNull() ? "暂无" : String.valueOf(userRankNode.asInt());

        // Group items by strategy
        Map<String, StringBuilder> groups = new LinkedHashMap<>();
        groups.put("rush", new StringBuilder());
        groups.put("safe", new StringBuilder());
        groups.put("guarantee", new StringBuilder());
        for (JsonNode item : topItems) {
            String group = item.path("group").asText("safe");
            if (!groups.containsKey(group)) {
                group = "safe";
            }
            groups.get(group).append(renderItemRow(item));
        }

        sb.append("## 一、分数段研判\n");
        sb.append(String.format("%d分%s类%s考生，参考位次约%s。", score, subject, province, userRank));
        sb.append("以下推荐基于本系统数据库真实录取数据，冲稳保三档已按录取概率分层。\n\n");

        sb.append("## 二、冲稳保院校推荐\n\n");
        appendGroupTable(sb, "冲一冲", groups.get("rush"), score);
        appendGroupTable(sb, "稳一稳", groups.get("safe"), score);
        appendGroupTable(sb, "保一保", groups.get("guarantee"), score);

        sb.append("## 三、院校+专业匹配\n");
        int matchCount = 0;
        for (JsonNode item : topItems) {
            if (matchCount >= 3) break;
            String uni = item.path("universityName").asText("");
            String major = item.path("majorName").asText("");
            String tags = joinTags(item.path("schoolTags"));
            String line = major.isBlank()
                    ? String.format("- **%s**（%s）", uni, tags.isBlank() ? "普通院校" : tags)
                    : String.format("- **%s**：%s（%s）", uni, major, tags.isBlank() ? "普通院校" : tags);
            sb.append(line).append("\n");
            matchCount++;
        }
        sb.append("\n");

        sb.append("## 四、填报策略\n");
        sb.append("1. **冲一冲**：参考冲档院校，录取概率较低但可冲击更高层次。\n");
        sb.append("2. **稳一稳**：与分数匹配度最高，作为志愿表主体。\n");
        sb.append("3. **保一保**：保底院校，防止滑档。\n\n");

        sb.append("## 五、风险提醒\n");
        sb.append("- **选科要求**：核对目标院校专业的选科要求是否与自身组合匹配。\n");
        sb.append("- **调剂风险**：建议勾选服从调剂，降低退档概率。\n");
        sb.append("- **位次趋势**：结合近三年录取位次趋势综合判断，避免单看分数。\n\n");

        sb.append("## 六、总结\n");
        sb.append(String.format("%d分%s类%s考生，建议按冲稳保梯度组合志愿，", score, subject, province));
        int total = topItems.size();
        sb.append(String.format("本次共匹配%d所院校。请核对招生章程、科目限制与近三年位次后再填报。\n", total));

        return sb.toString();
    }

    private String renderItemRow(JsonNode item) {
        String uni = item.path("universityName").asText("—");
        String major = item.path("majorName").asText("");
        String tags = joinTags(item.path("schoolTags"));
        int cutoff = item.path("cutoffScore").asInt(0);
        int prob = item.path("admissionProbability").asInt(0);
        String probStr = prob > 0 ? prob + "%" : "—";
        String majorStr = major.isBlank() ? "—" : major;
        String tagsStr = tags.isBlank() ? "普通" : tags;
        return String.format("| %s | %s | %s | %d | %s |\n", uni, majorStr, tagsStr, cutoff, probStr);
    }

    private void appendGroupTable(StringBuilder sb, String title, StringBuilder rows, int score) {
        if (rows.length() == 0) {
            return;
        }
        sb.append(String.format("### %s\n", title));
        sb.append("| 院校 | 专业 | 层次 | 录取分 | 录取概率 |\n");
        sb.append("|---|---|---|---|---|\n");
        sb.append(rows);
        sb.append("\n");
    }

    private String joinTags(JsonNode tagsNode) {
        if (tagsNode == null || !tagsNode.isArray() || tagsNode.isEmpty()) {
            return "";
        }
        StringBuilder sb = new StringBuilder();
        Iterator<JsonNode> it = tagsNode.elements();
        while (it.hasNext()) {
            if (sb.length() > 0) sb.append("/");
            sb.append(it.next().asText(""));
        }
        return sb.toString();
    }

    private JsonNode parsePayload(String payloadJson) {
        if (payloadJson == null || payloadJson.isBlank()) {
            return null;
        }
        try {
            return objectMapper.readTree(payloadJson);
        } catch (Exception ex) {
            log.warn("Failed to parse agent tool payload: {}", ex.getMessage());
            return null;
        }
    }
}
