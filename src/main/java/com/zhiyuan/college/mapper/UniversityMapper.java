package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.University;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UniversityMapper extends BaseMapper<University> {

    @Select("""
            SELECT id,
                   name,
                   province,
                   tier,
                   is_985 AS is985,
                   is_211 AS is211,
                   is_double_first_class AS isDoubleFirstClass,
                   tags
            FROM university
            WHERE id = #{id}
            """)
    University findById(@Param("id") Long id);

    @Select("""
            SELECT id,
                   name,
                   province,
                   tier,
                   is_985 AS is985,
                   is_211 AS is211,
                   is_double_first_class AS isDoubleFirstClass,
                   tags
            FROM university
            WHERE name = #{name}
            LIMIT 1
            """)
    University findByExactName(@Param("name") String name);

    @Select("""
            SELECT id,
                   name,
                   province,
                   tier,
                   is_985 AS is985,
                   is_211 AS is211,
                   is_double_first_class AS isDoubleFirstClass,
                   tags
            FROM university
            ORDER BY id
            """)
    List<University> findAllOrdered();

    @Select("""
            <script>
            SELECT id, name, province, tier,
                   is_985 AS is985, is_211 AS is211,
                   is_double_first_class AS isDoubleFirstClass, tags
            FROM university
            <where>
              <if test="province != null and province != ''">AND province = #{province}</if>
              <if test="keyword != null and keyword != ''">AND name LIKE CONCAT('%', #{keyword}, '%')</if>
              <if test="tag != null and tag != ''">AND tags LIKE CONCAT('%', #{tag}, '%')</if>
            </where>
            ORDER BY id
            </script>
            """)
    List<University> findForPublicList(@Param("province") String province,
                                      @Param("keyword") String keyword,
                                      @Param("tag") String tag);

    @Select("""
            <script>
            SELECT id, name, province, tier,
                   is_985 AS is985, is_211 AS is211,
                   is_double_first_class AS isDoubleFirstClass, tags
            FROM university
            WHERE id IN
            <foreach item="item" collection="ids" open="(" separator="," close=")">#{item}</foreach>
            ORDER BY id
            </script>
            """)
    List<University> findByIds(@Param("ids") List<Long> ids);

    @Select("""
            SELECT DISTINCT province
            FROM university
            WHERE province IS NOT NULL AND province != ''
            ORDER BY province
            """)
    List<String> findDistinctProvinces();

    @Select("""
            SELECT DISTINCT tags
            FROM university
            WHERE tags IS NOT NULL AND tags != ''
            ORDER BY tags
            """)
    List<String> findDistinctTagValues();
}
