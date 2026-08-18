package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.dto.SchoolMajorItemResponse;
import com.zhiyuan.college.model.entity.MajorAdmissionCutoff;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MajorAdmissionCutoffMapper extends BaseMapper<MajorAdmissionCutoff> {

    @Select("""
            SELECT m.id,
                   m.university_id AS universityId,
                   m.major_id AS majorId,
                   u.name AS universityName,
                   COALESCE(maj.name, m.major_name) AS majorName,
                   u.province AS universityProvince,
                   u.tier AS universityTier,
                   u.is_985 AS is985,
                   u.is_211 AS is211,
                   u.is_double_first_class AS isDoubleFirstClass,
                   u.tags AS universityTags,
                   m.admission_year AS admissionYear,
                   m.province,
                   m.subject_type AS subjectType,
                   m.cutoff_score AS cutoffScore,
                   m.min_rank AS minRank
            FROM major_admission_cutoff m
            JOIN university u ON m.university_id = u.id
            LEFT JOIN major maj ON maj.id = m.major_id
            WHERE m.province = #{province}
              AND m.subject_type = #{subjectType}
              AND LOWER(COALESCE(maj.name, m.major_name)) LIKE CONCAT('%', LOWER(#{majorKeyword}), '%')
              AND m.admission_year = (
                SELECT MAX(m2.admission_year)
                FROM major_admission_cutoff m2
                WHERE m2.province = #{province}
                  AND m2.subject_type = #{subjectType}
                  AND m2.university_id = #{universityId}
              )
            """)
    List<AdmissionCutoffWithUniversity> findLatestByProvinceSubjectAndMajorKeyword(@Param("province") String province,
                                                                                    @Param("subjectType") String subjectType,
                                                                                    @Param("majorKeyword") String majorKeyword);

    @Select("""
            SELECT COALESCE(maj.name, m.major_name) AS majorName,
                   m.cutoff_score AS cutoffScore,
                   m.min_rank AS minRank
            FROM major_admission_cutoff m
            LEFT JOIN major maj ON maj.id = m.major_id
            WHERE m.university_id = #{universityId}
              AND m.province = #{province}
              AND m.subject_type = #{subjectType}
              AND m.admission_year = (
                SELECT MAX(m2.admission_year)
                FROM major_admission_cutoff m2
                WHERE m2.province = #{province}
                  AND m2.subject_type = #{subjectType}
              )
            ORDER BY
              CASE WHEN m.cutoff_score IS NULL THEN 1 ELSE 0 END,
              m.cutoff_score DESC,
              CASE WHEN m.min_rank IS NULL THEN 1 ELSE 0 END,
              m.min_rank ASC,
              COALESCE(maj.name, m.major_name) ASC
            """)
    List<SchoolMajorItemResponse> findLatestMajorsByUniversityIdAndProvinceAndSubject(@Param("universityId") Long universityId,
                                                                                       @Param("province") String province,
                                                                                       @Param("subjectType") String subjectType);

    @Select("""
            <script>
            SELECT id,
                   university_id AS universityId,
                   major_id AS majorId,
                   major_name AS majorName,
                   admission_year AS admissionYear,
                   province,
                   subject_type AS subjectType,
                   cutoff_score AS cutoffScore,
                   min_rank AS minRank
            FROM major_admission_cutoff
            WHERE 1 = 1
            <if test="universityId != null">
              AND university_id = #{universityId}
            </if>
            <if test="province != null and province != ''">
              AND province = #{province}
            </if>
            <if test="subjectType != null and subjectType != ''">
              AND subject_type = #{subjectType}
            </if>
            <if test="admissionYear != null">
              AND admission_year = #{admissionYear}
            </if>
            <if test="majorKeyword != null and majorKeyword != ''">
              AND LOWER(major_name) LIKE CONCAT('%', LOWER(#{majorKeyword}), '%')
            </if>
            ORDER BY admission_year DESC, university_id ASC, major_name ASC, id DESC
            </script>
            """)
    List<MajorAdmissionCutoff> findAdminList(@Param("universityId") Long universityId,
                                             @Param("province") String province,
                                             @Param("subjectType") String subjectType,
                                             @Param("admissionYear") Integer admissionYear,
                                             @Param("majorKeyword") String majorKeyword);

    @Select("""
            SELECT id, university_id AS universityId, major_id AS majorId,
                   major_name AS majorName, admission_year AS admissionYear,
                   province, subject_type AS subjectType,
                   cutoff_score AS cutoffScore, min_rank AS minRank
            FROM major_admission_cutoff
            WHERE university_id = #{universityId}
              AND province = #{province}
              AND subject_type = #{subjectType}
              AND major_name = #{majorName}
            ORDER BY admission_year DESC
            LIMIT 1
            """)
    MajorAdmissionCutoff findLatestByUniversityAndMajor(@Param("universityId") Long universityId,
                                                       @Param("province") String province,
                                                       @Param("subjectType") String subjectType,
                                                       @Param("majorName") String majorName);

    @Select("""
            SELECT id, university_id AS universityId, major_id AS majorId,
                   major_name AS majorName, admission_year AS admissionYear,
                   province, subject_type AS subjectType,
                   cutoff_score AS cutoffScore, min_rank AS minRank
            FROM major_admission_cutoff
            WHERE university_id = #{universityId}
              AND province = #{province}
              AND subject_type = #{subjectType}
            ORDER BY admission_year DESC, cutoff_score DESC
            """)
    List<MajorAdmissionCutoff> findAllByUniversityAndProvinceSubject(@Param("universityId") Long universityId,
                                                                    @Param("province") String province,
                                                                    @Param("subjectType") String subjectType);
}
