package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.UserAccount;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT id, username, password, score, created_at, updated_at FROM users WHERE username = #{username} LIMIT 1")
    UserAccount findByUsername(@Param("username") String username);
}

