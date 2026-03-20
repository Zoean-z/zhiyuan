package com.zhiyuan.college.service;

import com.zhiyuan.college.mapper.UniversityMapper;
import com.zhiyuan.college.model.dto.SchoolDetailResponse;
import com.zhiyuan.college.model.dto.SchoolMajorItemResponse;
import com.zhiyuan.college.model.entity.University;
import com.zhiyuan.college.util.UniversityTagUtils;
import java.util.List;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SchoolDetailService {

    private final UniversityMapper universityMapper;
    private final JdbcTemplate jdbcTemplate;

    public SchoolDetailService(UniversityMapper universityMapper, JdbcTemplate jdbcTemplate) {
        this.universityMapper = universityMapper;
        this.jdbcTemplate = jdbcTemplate;
    }

    public SchoolDetailResponse getSchoolDetail(Long universityId, String province, String subjectType) {
        University university = universityMapper.findById(universityId);
        if (university == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "school not found");
        }

        List<SchoolMajorItemResponse> majors = queryMajors(
                """
                SELECT major_name, cutoff_score, min_rank
                FROM major_admission_cutoff
                WHERE university_id = ?
                  AND province = ?
                  AND subject_type = ?
                ORDER BY admission_year DESC,
                         CASE WHEN cutoff_score IS NULL THEN 1 ELSE 0 END,
                         cutoff_score DESC,
                         CASE WHEN min_rank IS NULL THEN 1 ELSE 0 END,
                         min_rank ASC,
                         major_name ASC
                """,
                universityId,
                province,
                subjectType
        );

        if (majors.isEmpty()) {
            majors = queryMajors(
                    """
                    SELECT major_name, cutoff_score, min_rank
                    FROM major_admission_cutoff
                    WHERE university_id = ?
                    ORDER BY admission_year DESC,
                             CASE WHEN cutoff_score IS NULL THEN 1 ELSE 0 END,
                             cutoff_score DESC,
                             CASE WHEN min_rank IS NULL THEN 1 ELSE 0 END,
                             min_rank ASC,
                             major_name ASC
                    """,
                    universityId
            );
        }

        return new SchoolDetailResponse(
                university.getId(),
                university.getName(),
                university.getProvince(),
                university.getTier(),
                Boolean.TRUE.equals(university.getIs985()),
                Boolean.TRUE.equals(university.getIs211()),
                Boolean.TRUE.equals(university.getIsDoubleFirstClass()),
                UniversityTagUtils.buildSchoolTags(
                        university.getIs985(),
                        university.getIs211(),
                        university.getIsDoubleFirstClass(),
                        university.getTier()),
                university.getTags(),
                majors
        );
    }

    private List<SchoolMajorItemResponse> queryMajors(String sql, Object... args) {
        return jdbcTemplate.query(
                sql,
                (rs, rowNum) -> new SchoolMajorItemResponse(
                        rs.getString("major_name"),
                        (Integer) rs.getObject("cutoff_score"),
                        (Integer) rs.getObject("min_rank")),
                args
        );
    }
}
