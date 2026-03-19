package com.zhiyuan.college.mapper;

import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MajorAdmissionCutoffMapper {

    @Select("""
            SELECT m.id,
                   m.university_id AS universityId,
                   u.name AS universityName,
                   m.major_name AS majorName,
                   u.province AS universityProvince,
                   u.tier AS universityTier,
                   u.tags AS universityTags,
                   m.admission_year AS admissionYear,
                   m.province,
                   m.subject_type AS subjectType,
                   m.cutoff_score AS cutoffScore,
                   m.min_rank AS minRank
            FROM major_admission_cutoff m
            JOIN university u ON m.university_id = u.id
            WHERE m.province = #{province}
              AND m.subject_type = #{subjectType}
              AND LOWER(m.major_name) LIKE CONCAT('%', LOWER(#{majorKeyword}), '%')
              AND m.admission_year = (
                SELECT MAX(m2.admission_year)
                FROM major_admission_cutoff m2
                WHERE m2.province = #{province}
                  AND m2.subject_type = #{subjectType}
              )
            """)
    List<AdmissionCutoffWithUniversity> findLatestByProvinceSubjectAndMajorKeyword(@Param("province") String province,
                                                                                    @Param("subjectType") String subjectType,
                                                                                    @Param("majorKeyword") String majorKeyword);

    @Select("""
            <script>
            SELECT DISTINCT m.major_name
            FROM major_admission_cutoff m
            WHERE LOWER(m.major_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            <if test="province != null and province != ''">
              AND m.province = #{province}
            </if>
            <if test="subjectType != null and subjectType != ''">
              AND m.subject_type = #{subjectType}
            </if>
            ORDER BY
              CASE
                WHEN LOWER(m.major_name) = LOWER(#{keyword}) THEN 0
                WHEN LOWER(m.major_name) LIKE CONCAT(LOWER(#{keyword}), '%') THEN 1
                ELSE 2
              END,
              LENGTH(m.major_name),
              m.major_name
            LIMIT 10
            </script>
            """)
    List<String> findMajorSuggestions(@Param("keyword") String keyword,
                                      @Param("province") String province,
                                      @Param("subjectType") String subjectType);
}
