package com.zhiyuan.college.service;

import org.springframework.stereotype.Service;

@Service
public class AiAdviceSummaryService {

    public String summarize(String adviceText) {
        String normalized = adviceText == null ? "" : adviceText.replace("\n", " ").trim();
        if (normalized.isEmpty()) {
            return "暂无可总结内容。";
        }
        if (normalized.length() <= 80) {
            return "AI总结：" + normalized;
        }
        return "AI总结：" + normalized.substring(0, 80) + "。建议按“冲稳保”比例完成最终志愿排序并核验近三年位次。";
    }
}
