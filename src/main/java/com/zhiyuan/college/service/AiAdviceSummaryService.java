package com.zhiyuan.college.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

@Service
public class AiAdviceSummaryService {

    private static final Logger log = LoggerFactory.getLogger(AiAdviceSummaryService.class);

    private final QwenAiClient qwenAiClient;
    private final boolean enabled;

    public AiAdviceSummaryService(QwenAiClient qwenAiClient,
                                  @Value("${ai.qwen.enabled:true}") boolean enabled) {
        this.qwenAiClient = qwenAiClient;
        this.enabled = enabled;
    }

    public String summarize(String adviceText) {
        if (enabled) {
            try {
                String systemPrompt = """
                        你是高考志愿填报顾问。请将输入建议总结为 60-120 字中文，语言清晰，包含策略重点和风险提示。
                        只输出总结内容，不要加标题。
                        """;
                String userPrompt = "原始建议：\n" + (adviceText == null ? "" : adviceText);
                String result = qwenAiClient.chat(systemPrompt, userPrompt, 0.3, false);
                if (result != null && !result.isBlank()) {
                    return result.trim();
                }
            } catch (Exception ex) {
                log.warn("Qwen summary failed, fallback to local summary: {}", ex.getMessage());
            }
        }

        String normalized = adviceText == null ? "" : adviceText.replace("\n", " ").trim();
        if (normalized.isEmpty()) {
            return "暂无可总结内容。";
        }
        if (normalized.length() <= 80) {
            return "AI总结：" + normalized;
        }
        return "AI总结：" + normalized.substring(0, 80) + "。建议按‘冲稳保’比例完成最终志愿排序并核验近三年位次。";
    }
}
