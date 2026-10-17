package com.zhiyuan.college.service.agent;

import com.zhiyuan.college.util.AdmissionAssessmentDisplay;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.zhiyuan.college.model.entity.UserAccount;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
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
            // 校名/序号查校详情（getSchoolDetail、getSchoolDetailByName）：载荷带
            // universityName + majors 数组，渲染成校情卡片而不是回显一行摘要。
            JsonNode majors = payload.path("majors");
            if (!payload.path("universityName").asText("").isBlank() && majors.isArray()) {
                return renderSchoolDetailMarkdown(payload, majors, toolResult);
            }
            return toolResult.getSummary();
        }
        return renderRecommendationMarkdown(payload, topItems, user);
    }

    /**
     * 校情卡片：院校层次/省份 + 可参考专业表（载荷里是按参考录取分从高到低的
     * 前 8 条，录取分较高通常对应报考热度更高），附下一步引导。
     * 完整专业清单仍留在消息库，聊天里给前 8 条足够决策参考。
     */
    private String renderSchoolDetailMarkdown(JsonNode payload, JsonNode majors, AgentToolResult toolResult) {
        String universityName = payload.path("universityName").asText("");
        if (majors.isEmpty()) {
            return toolResult.getSummary();
        }
        StringBuilder sb = new StringBuilder();
        sb.append("## ").append(universityName).append("\n\n");
        String tierLine = buildTierLine(payload);
        if (!tierLine.isBlank()) {
            sb.append(tierLine).append("\n");
        }
        String province = payload.path("universityProvince").asText("");
        if (!province.isBlank()) {
            sb.append("所在省份：").append(province).append("\n");
        }
        if (!tierLine.isBlank() || !province.isBlank()) {
            sb.append("\n");
        }

        int total = payload.path("majorCount").asInt(majors.size());
        sb.append("### 可参考专业（共 %d 个，以下按参考录取分从高到低展示）\n\n".formatted(total));
        sb.append("| 专业 | 参考录取分 | 最低位次 |\n");
        sb.append("|---|---|---|\n");
        for (JsonNode major : majors) {
            String name = major.path("majorName").asText("—");
            int cutoff = major.path("cutoffScore").asInt(0);
            int minRank = major.path("minRank").asInt(0);
            sb.append(String.format("| %s | %s | %s |\n", name,
                    cutoff > 0 ? String.valueOf(cutoff) : "—",
                    minRank > 0 ? String.valueOf(minRank) : "—"));
        }
        sb.append("\n");

        sb.append("### 下一步\n");
        sb.append("- 录取分较高的专业通常报考热度也更高；选科要求与招生计划请以当年招生章程为准。\n");
        sb.append("- 想了解某个专业的学习内容与就业方向，直接问我，例如“电子信息工程专业怎么样”。\n");
        sb.append("- 生成学校推荐后，可以说“把第 N 所加入志愿单”把该校加入志愿表。\n");
        return sb.toString();
    }

    /** 院校层次行：985/211/双一流 优先，schoolTags 去重补充；全空时退回 universityTier。 */
    private String buildTierLine(JsonNode payload) {
        List<String> labels = new ArrayList<>();
        if (payload.path("is985").asBoolean(false)) {
            labels.add("985");
        }
        if (payload.path("is211").asBoolean(false)) {
            labels.add("211");
        }
        if (payload.path("isDoubleFirstClass").asBoolean(false)) {
            labels.add("双一流");
        }
        JsonNode schoolTags = payload.path("schoolTags");
        if (schoolTags.isArray()) {
            for (JsonNode tag : schoolTags) {
                String text = tag.asText("").trim();
                if (!text.isBlank() && !labels.contains(text)) {
                    labels.add(text);
                }
            }
        }
        if (labels.isEmpty()) {
            String tier = payload.path("universityTier").asText("").trim();
            return tier.isBlank() ? "" : "院校层次：" + tier;
        }
        return "院校层次：" + String.join(" / ", labels);
    }

    private String renderRecommendationMarkdown(JsonNode payload, JsonNode topItems, UserAccount user) {
        StringBuilder sb = new StringBuilder();
        int score = user != null && user.getScore() != null ? user.getScore() : 0;
        String subject = user != null && user.getSubjectType() != null ? user.getSubjectType().getDisplayName() : "未知";
        String province = user != null && user.getExamProvince() != null ? user.getExamProvince() : "未知";
        JsonNode userRankNode = payload.path("userRank");
        String userRank = userRankNode.isMissingNode() || userRankNode.isNull() ? "暂无" : String.valueOf(userRankNode.asInt());
        boolean majorFirst = "MAJOR_FIRST".equals(payload.path("recommendationMode").asText(""));

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
        sb.append("以下推荐基于本系统数据库真实录取数据，冲稳保三档已按规则参考机会分层。\n\n");

        sb.append(majorFirst ? "## 二、冲稳保专业推荐\n\n" : "## 二、冲稳保院校推荐\n\n");
        appendGroupTable(sb, "冲一冲", groups.get("rush"), score);
        appendGroupTable(sb, "稳一稳", groups.get("safe"), score);
        appendGroupTable(sb, "保一保", groups.get("guarantee"), score);

        sb.append(majorFirst ? "## 三、专业与开设院校\n" : "## 三、院校+专业匹配\n");
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
        if (majorFirst) {
            sb.append(String.format("本次围绕你选择的专业方向共给出%d条专业推荐。请核对招生章程、科目限制与近三年位次后再填报。\n", total));
        } else {
            sb.append(String.format("本次共匹配%d所院校。请核对招生章程、科目限制与近三年位次后再填报。\n", total));
        }

        return sb.toString();
    }

    private String renderItemRow(JsonNode item) {
        String uni = item.path("universityName").asText("—");
        String major = item.path("majorName").asText("");
        String tags = joinTags(item.path("schoolTags"));
        int cutoff = item.path("cutoffScore").asInt(0);
        int prob = item.path("admissionProbability").asInt(0);
        String probStr = item.path("admissionProbability").isNumber() ? AdmissionAssessmentDisplay.level(prob) : "待评估";
        String majorStr = major.isBlank() ? "—" : major;
        String tagsStr = tags.isBlank() ? "普通" : tags;
        return String.format("| %s | %s | %s | %d | %s |\n", uni, majorStr, tagsStr, cutoff, probStr);
    }

    private void appendGroupTable(StringBuilder sb, String title, StringBuilder rows, int score) {
        if (rows.length() == 0) {
            return;
        }
        sb.append(String.format("### %s\n", title));
        sb.append("| 院校 | 专业 | 层次 | 录取分 | 机会等级 |\n");
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
