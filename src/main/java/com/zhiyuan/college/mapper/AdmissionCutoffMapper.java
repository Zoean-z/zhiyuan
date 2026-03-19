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
                   u.tags AS universityTags,
                   c.admission_year AS admissionYear,
                   c.province,
                   c.subject_type AS subjectType,
                   c.cutoff_score AS cutoffScore,
                   c.min_rank AS minRank
            FROM admission_cutoff c
            JOIN university u ON c.university_id = u.id
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
}
