package com.zhiyuan.college.mapper;

import com.zhiyuan.college.model.entity.University;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UniversityMapper {

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
}
