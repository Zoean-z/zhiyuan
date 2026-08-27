package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.dto.AdmissionCutoffWithUniversity;
import com.zhiyuan.college.model.entity.AdmissionCutoff;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface AdmissionCutoffMapper extends BaseMapper<AdmissionCutoff> {

    @Select("""
            SELECT c.id,
                   c.university_id AS universityId,
                   u.name AS universityName,
                   u.province AS universityProvince,
                   u.tier AS universityTier,
                   CASE WHEN u.tier = '985' THEN TRUE ELSE FALSE END AS is985,
                   CASE WHEN u.tier IN ('985', '211') THEN TRUE ELSE FALSE END AS is211,
                   CASE WHEN u.tier IN ('985', '211', '双一流') THEN TRUE ELSE FALSE END AS isDoubleFirstClass,
                   u.tags AS universityTags,
                   c.admission_year AS admissionYear,
                   c.province,
                   c.subject_type AS subjectType,
                   c.cutoff_score AS cutoffScore,
                   c.min_rank AS minRank,
                   source_cutoff.data_kind AS dataKind,
                   source_cutoff.calibration_source AS calibrationSource,
                   source_cutoff.simulation_rule AS simulationRule
            FROM admission_cutoff c
            JOIN university u ON c.university_id = u.id
            LEFT JOIN major_admission_cutoff source_cutoff ON source_cutoff.id = (
                SELECT m.id
                FROM major_admission_cutoff m
                WHERE m.university_id = c.university_id
                  AND m.admission_year = c.admission_year
                  AND m.province = c.province
                  AND m.subject_type = c.subject_type
                ORDER BY CASE WHEN m.cutoff_score = c.cutoff_score THEN 0 ELSE 1 END,
                         m.cutoff_score ASC,
                         m.id ASC
                LIMIT 1
            )
            WHERE c.province = #{province}
              AND c.subject_type = #{subjectType}
              AND c.admission_year = (
                SELECT MAX(c2.admission_year)
                FROM admission_cutoff c2
                WHERE c2.province = #{province}
                  AND c2.subject_type = #{subjectType}
              )
            """)
    List<AdmissionCutoffWithUniversity> findLatestByProvinceAndSubject(@Param("province") String province,
                                                                       @Param("subjectType") String subjectType);

    @Select("""
            SELECT c.id,
                   c.university_id AS universityId,
                   u.name AS universityName,
                   u.province AS universityProvince,
                   u.tier AS universityTier,
                   CASE WHEN u.tier = '985' THEN TRUE ELSE FALSE END AS is985,
                   CASE WHEN u.tier IN ('985', '211') THEN TRUE ELSE FALSE END AS is211,
                   CASE WHEN u.tier IN ('985', '211', '双一流') THEN TRUE ELSE FALSE END AS isDoubleFirstClass,
                   u.tags AS universityTags,
                   c.admission_year AS admissionYear,
                   c.province,
                   c.subject_type AS subjectType,
                   c.cutoff_score AS cutoffScore,
                   c.min_rank AS minRank
            FROM admission_cutoff c
            JOIN university u ON c.university_id = u.id
            WHERE c.province = #{province}
              AND c.admission_year = (
                SELECT MAX(c2.admission_year)
                FROM admission_cutoff c2
                WHERE c2.province = #{province}
              )
            """)
    List<AdmissionCutoffWithUniversity> findLatestByProvince(@Param("province") String province);

    @Select("SELECT DISTINCT province FROM admission_cutoff ORDER BY province")
    List<String> findDistinctProvinces();

    @Select("""
            <script>
            SELECT id,
                   university_id AS universityId,
                   admission_year AS admissionYear,
                   province,
                   subject_type AS subjectType,
                   cutoff_score AS cutoffScore,
                   min_rank AS minRank
            FROM admission_cutoff
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
            ORDER BY admission_year DESC, university_id ASC, subject_type ASC, id DESC
            </script>
            """)
    List<AdmissionCutoff> findAdminList(@Param("universityId") Long universityId,
                                        @Param("province") String province,
                                        @Param("subjectType") String subjectType,
                                        @Param("admissionYear") Integer admissionYear);

    @Select("""
            SELECT c.id, c.university_id AS universityId, c.admission_year AS admissionYear,
                   c.province, c.subject_type AS subjectType,
                   c.cutoff_score AS cutoffScore, c.min_rank AS minRank,
                   source_cutoff.data_kind AS dataKind,
                   source_cutoff.calibration_source AS calibrationSource,
                   source_cutoff.simulation_rule AS simulationRule
            FROM admission_cutoff c
            LEFT JOIN major_admission_cutoff source_cutoff ON source_cutoff.id = (
                SELECT m.id
                FROM major_admission_cutoff m
                WHERE m.university_id = c.university_id
                  AND m.admission_year = c.admission_year
                  AND m.province = c.province
                  AND m.subject_type = c.subject_type
                ORDER BY CASE WHEN m.cutoff_score = c.cutoff_score THEN 0 ELSE 1 END,
                         m.cutoff_score ASC,
                         m.id ASC
                LIMIT 1
            )
            WHERE c.university_id = #{universityId}
              AND c.province = #{province}
              AND c.subject_type = #{subjectType}
            ORDER BY c.admission_year DESC
            LIMIT 3
            """)
    List<AdmissionCutoff> findHistoryByUniversityAndProvinceSubject(@Param("universityId") Long universityId,
                                                                   @Param("province") String province,
                                                                   @Param("subjectType") String subjectType);

    @Select("""
            SELECT ac.id, ac.university_id AS universityId, ac.admission_year AS admissionYear,
                   ac.province, ac.subject_type AS subjectType,
                   ac.cutoff_score AS cutoffScore, ac.min_rank AS minRank,
                   source_cutoff.data_kind AS dataKind,
                   source_cutoff.calibration_source AS calibrationSource,
                   source_cutoff.simulation_rule AS simulationRule
            FROM admission_cutoff ac
            JOIN (
                SELECT university_id, MAX(admission_year) AS latest_year
                FROM admission_cutoff
                WHERE province = #{province}
                  AND subject_type = #{subjectType}
                GROUP BY university_id
            ) latest ON latest.university_id = ac.university_id
                    AND latest.latest_year = ac.admission_year
            LEFT JOIN major_admission_cutoff source_cutoff ON source_cutoff.id = (
                SELECT m.id
                FROM major_admission_cutoff m
                WHERE m.university_id = ac.university_id
                  AND m.admission_year = ac.admission_year
                  AND m.province = ac.province
                  AND m.subject_type = ac.subject_type
                ORDER BY CASE WHEN m.cutoff_score = ac.cutoff_score THEN 0 ELSE 1 END,
                         m.cutoff_score ASC,
                         m.id ASC
                LIMIT 1
            )
            WHERE ac.province = #{province}
              AND ac.subject_type = #{subjectType}
            """)
    List<AdmissionCutoff> findLatestPerUniversity(@Param("province") String province,
                                                 @Param("subjectType") String subjectType);

    @Select("""
            SELECT id, university_id AS universityId, admission_year AS admissionYear,
                   province, subject_type AS subjectType,
                   cutoff_score AS cutoffScore, min_rank AS minRank
            FROM admission_cutoff
            WHERE university_id = #{universityId}
              AND province = #{province}
              AND subject_type = #{subjectType}
            ORDER BY admission_year DESC
            LIMIT 1
            """)
    AdmissionCutoff findLatestByUniversityAndProvinceSubject(@Param("universityId") Long universityId,
                                                            @Param("province") String province,
                                                            @Param("subjectType") String subjectType);
}
