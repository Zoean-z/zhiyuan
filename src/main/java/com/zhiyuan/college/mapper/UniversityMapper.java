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
}
