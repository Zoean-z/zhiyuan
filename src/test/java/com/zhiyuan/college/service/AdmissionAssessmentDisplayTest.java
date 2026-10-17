package com.zhiyuan.college.service;

import com.zhiyuan.college.util.AdmissionAssessmentDisplay;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class AdmissionAssessmentDisplayTest {
    @Test void coversAllSevenDisplayLevelsAndTheirBoundaries() {
        double[] values = {-1, 0, 0.1, 19.9, 20, 34.9, 35, 54.9, 55, 74.9, 75, 89.9, 90, 96};
        String[] expected = {"极低", "极低", "较低", "较低", "低", "低", "中", "中", "高", "高", "较高", "较高", "极高", "极高"};
        for (int i = 0; i < values.length; i++) assertEquals(expected[i], AdmissionAssessmentDisplay.level(values[i]));
        assertEquals("待评估", AdmissionAssessmentDisplay.level(Double.NaN));
        assertEquals("录取机会较低；录取机会较高", AdmissionAssessmentDisplay.text("录取概率18%；录取概率80%"));
    }
    @Test void preservesFactsAndConvertsKnownAdmissionOutput() {
        String converted = AdmissionAssessmentDisplay.text("| 院校 | 录取概率 | 保研率 |\n|---|---|---|\n| A | 62% | 20% |\n规则测算录取概率约为 96%；风险指数 38/100；最低分591");
        assertTrue(converted.contains("| A | 高 | 20% |"));
        assertTrue(converted.contains("录取机会极高"));
        assertTrue(converted.contains("最低分591"));
        assertFalse(converted.contains("38/100"));
        assertEquals("待评估", AdmissionAssessmentDisplay.level(null));
        assertEquals("极低", AdmissionAssessmentDisplay.level(0));
    }
}
