package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT id, username, password, score, subject_type AS subject_type_value, exam_province, created_at, updated_at FROM users WHERE username = #{username} LIMIT 1")
    UserAccount findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, score, subject_type AS subject_type_value, exam_province, created_at, updated_at FROM users WHERE id = #{id} LIMIT 1")
    UserAccount findByIdCompat(@Param("id") Long id);
}
