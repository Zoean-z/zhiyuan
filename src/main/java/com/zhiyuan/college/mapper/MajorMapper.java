package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.Major;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface MajorMapper extends BaseMapper<Major> {

    @Select("""
            SELECT id,
                   name,
                   category,
                   degree_type AS degreeType,
                   tags,
                   subject_requirement AS subjectRequirement,
                   description,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM major
            ORDER BY name ASC, id ASC
            """)
    List<Major> findAllOrdered();

    @Select("""
            SELECT id,
                   name,
                   category,
                   degree_type AS degreeType,
                   tags,
                   subject_requirement AS subjectRequirement,
                   description,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM major
            WHERE id = #{id}
            LIMIT 1
            """)
    Major findByIdCompat(@Param("id") Long id);

    @Select("""
            SELECT id,
                   name,
                   category,
                   degree_type AS degreeType,
                   tags,
                   subject_requirement AS subjectRequirement,
                   description,
                   created_at AS createdAt,
                   updated_at AS updatedAt
            FROM major
            WHERE LOWER(name) = LOWER(#{name})
            LIMIT 1
            """)
    Major findByExactName(@Param("name") String name);

    @Select("""
            <script>
            SELECT DISTINCT mac.major_name
            FROM major_admission_cutoff mac
            WHERE mac.major_name IS NOT NULL
              AND TRIM(mac.major_name) != ''
              AND LOWER(mac.major_name) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            <if test="province != null and province != ''">
              AND mac.province = #{province}
            </if>
            <if test="subjectType != null and subjectType != ''">
              AND mac.subject_type = #{subjectType}
            </if>
            ORDER BY
              CASE
                WHEN LOWER(mac.major_name) = LOWER(#{keyword}) THEN 0
                WHEN LOWER(mac.major_name) LIKE CONCAT(LOWER(#{keyword}), '%') THEN 1
                ELSE 2
              END,
              LENGTH(mac.major_name),
              mac.major_name
            LIMIT 10
            </script>
            """)
    List<String> findSuggestions(@Param("keyword") String keyword,
                                 @Param("province") String province,
                                 @Param("subjectType") String subjectType);
}
