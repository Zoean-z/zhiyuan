package com.zhiyuan.college.util;

import java.util.regex.Pattern;

/** Presentation only. Never feeds back into recommendation scoring. */
public final class AdmissionAssessmentDisplay {
    private AdmissionAssessmentDisplay() {}

    public static String level(Number value) {
        if (value == null) return "待评估";
        double p = value.doubleValue();
        if (!Double.isFinite(p)) return "待评估";
        if (p <= 0) return "极低";
        if (p < 20) return "较低";
        if (p < 35) return "低";
        if (p < 55) return "中";
        if (p < 75) return "高";
        if (p < 90) return "较高";
        return "极高";
    }

    /** Converts identified admission output, without removing factual rates such as employment rates. */
    public static String text(String input) {
        if (input == null) return null;
        String result = Pattern.compile("((?:规则测算|综合|参考)?录取概率(?:约为|约|为|是|[:：\\s])*?)(\\d+(?:\\.\\d+)?)\\s*[%％]")
                .matcher(input).replaceAll(m -> "录取机会" + level(Double.valueOf(m.group(2))));
        result = Pattern.compile("风险(?:指数|评分|分数)\\s*[:：]?\\s*\\d+(?:\\.\\d+)?\\s*(?:/\\s*100|分)?")
                .matcher(result).replaceAll("请结合位次差与招生条件判断风险");
        String[] lines = result.split("\\n", -1);
        int column = -1;
        for (int i = 0; i < lines.length; i++) {
            if (!lines[i].stripLeading().startsWith("|")) { column = -1; continue; }
            String[] cells = lines[i].split("\\|", -1);
            for (int j = 0; j < cells.length; j++) {
                if (cells[j].contains("录取概率") || cells[j].contains("机会等级")) {
                    column = j;
                    cells[j] = cells[j].replace("录取概率", "机会等级");
                }
            }
            if (column >= 0 && column < cells.length) {
                cells[column] = Pattern.compile("(\\d+(?:\\.\\d+)?)\\s*[%％]").matcher(cells[column])
                        .replaceAll(m -> level(Double.valueOf(m.group(1))));
            }
            lines[i] = String.join("|", cells);
        }
        return String.join("\n", lines);
    }
}
