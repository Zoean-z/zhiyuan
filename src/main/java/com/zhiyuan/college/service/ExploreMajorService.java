package com.zhiyuan.college.service;

import com.zhiyuan.college.model.dto.ExploreMajorDetailResponse;
import com.zhiyuan.college.model.dto.ExploreMajorSummaryResponse;
import com.zhiyuan.college.model.dto.MajorOfferingSchoolResponse;
import java.util.Arrays;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ExploreMajorService {

    private static final String SUMMARY_COLUMNS = """
            m.name, m.major_code, m.category, m.subcategory, m.duration, m.degree_type,
            m.gender_ratio, m.average_salary, m.popularity,
            COUNT(mo.university_id) AS offering_school_count
            """;

    private final JdbcTemplate jdbcTemplate;

    public ExploreMajorService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public List<ExploreMajorSummaryResponse> list(String keyword, String category) {
        String normalizedKeyword = keyword == null ? "" : keyword.trim();
        String normalizedCategory = category == null ? "" : category.trim();
        String sql = """
                SELECT %s
                FROM major m
                LEFT JOIN major_offering mo ON mo.major_id = m.id
                WHERE m.major_code IS NOT NULL
                  AND (? = '' OR LOWER(m.name) LIKE CONCAT('%%', LOWER(?), '%%') OR LOWER(m.major_code) LIKE CONCAT('%%', LOWER(?), '%%'))
                  AND (? = '' OR m.category = ?)
                GROUP BY m.id, m.name, m.major_code, m.category, m.subcategory, m.duration,
                         m.degree_type, m.gender_ratio, m.average_salary, m.popularity
                ORDER BY COALESCE(m.popularity, 999999), m.name
                """.formatted(SUMMARY_COLUMNS);
        return jdbcTemplate.query(sql, this::mapSummary,
                normalizedKeyword, normalizedKeyword, normalizedKeyword,
                normalizedCategory, normalizedCategory);
    }

    public ExploreMajorDetailResponse detail(String code) {
        String normalizedCode = normalizeCode(code);
        String sql = """
                SELECT %s, m.description, m.employment_directions, m.demo_data
                FROM major m
                LEFT JOIN major_offering mo ON mo.major_id = m.id
                WHERE m.major_code = ?
                GROUP BY m.id, m.name, m.major_code, m.category, m.subcategory, m.duration,
                         m.degree_type, m.gender_ratio, m.average_salary, m.popularity,
                         m.description, m.employment_directions, m.demo_data
                """.formatted(SUMMARY_COLUMNS);
        List<ExploreMajorDetailResponse> result = jdbcTemplate.query(sql, (rs, rowNum) -> {
            ExploreMajorSummaryResponse summary = mapSummary(rs, rowNum);
            String directions = rs.getString("employment_directions");
            List<String> employmentDirections = directions == null || directions.isBlank()
                    ? List.of()
                    : Arrays.stream(directions.split(",")).map(String::trim).filter(value -> !value.isBlank()).toList();
            return new ExploreMajorDetailResponse(
                    summary,
                    rs.getString("description"),
                    employmentDirections,
                    rs.getBoolean("demo_data")
            );
        }, normalizedCode);
        if (result.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Major not found");
        }
        return result.get(0);
    }

    public List<MajorOfferingSchoolResponse> offeringSchools(String code) {
        String normalizedCode = normalizeCode(code);
        detail(normalizedCode);
        String sql = """
                SELECT u.id, u.logo_id, u.name, u.province, u.city,
                       COALESCE(u.tags, '综合类') AS school_type,
                       COALESCE(u.nature, '公办') AS nature,
                       COALESCE(u.belong, '') AS belong,
                       u.is_985, u.is_211, u.is_double_first_class
                FROM major m
                JOIN major_offering mo ON mo.major_id = m.id
                JOIN university u ON u.id = mo.university_id
                WHERE m.major_code = ?
                ORDER BY COALESCE(u.logo_id, 999999), u.id
                """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> new MajorOfferingSchoolResponse(
                rs.getLong("id"),
                (Integer) rs.getObject("logo_id"),
                rs.getString("name"),
                rs.getString("province"),
                rs.getString("city"),
                rs.getString("school_type"),
                rs.getString("nature"),
                rs.getString("belong"),
                rs.getBoolean("is_985"),
                rs.getBoolean("is_211"),
                rs.getBoolean("is_double_first_class")
        ), normalizedCode);
    }

    private ExploreMajorSummaryResponse mapSummary(java.sql.ResultSet rs, int rowNum) throws java.sql.SQLException {
        return new ExploreMajorSummaryResponse(
                rs.getString("name"),
                rs.getString("major_code"),
                rs.getString("category"),
                rs.getString("subcategory"),
                rs.getString("duration"),
                rs.getString("degree_type"),
                rs.getString("gender_ratio"),
                rs.getString("average_salary"),
                (Integer) rs.getObject("popularity"),
                rs.getInt("offering_school_count")
        );
    }

    private String normalizeCode(String code) {
        if (code == null || code.isBlank() || code.length() > 16) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid major code");
        }
        return code.trim();
    }
}
