package com.zhiyuan.college.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
class AdmissionCutoffMapperTest {

    @Autowired
    private AdmissionCutoffMapper admissionCutoffMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void findLatest_shouldUseBooleanSchoolFlagsInsteadOfDerivingThemFromTier() {
        jdbcTemplate.update(
                "INSERT INTO university (id, name, province, tier, is_985, is_211, is_double_first_class, tags) VALUES (?, ?, ?, ?, ?, ?, ?, ?)",
                90001L, "事实来源测试大学", "测试省", "普通", false, true, true, "测试"
        );
        jdbcTemplate.update(
                "INSERT INTO admission_cutoff (id, university_id, admission_year, province, subject_type, cutoff_score, min_rank) VALUES (?, ?, ?, ?, ?, ?, ?)",
                90001L, 90001L, 2026, "测试省", "物理", 600, 10000
        );

        List<AdmissionCutoffWithUniversity> rows = admissionCutoffMapper.findLatestByProvinceAndSubject("测试省", "物理");

        assertEquals(1, rows.size());
        AdmissionCutoffWithUniversity row = rows.get(0);
        assertEquals("普通", row.getUniversityTier());
        assertFalse(row.getIs985());
        assertTrue(row.getIs211());
        assertTrue(row.getIsDoubleFirstClass());
    }
}
