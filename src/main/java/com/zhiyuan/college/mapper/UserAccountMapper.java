package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.dto.AdminUserOverviewResponse;
import com.zhiyuan.college.model.dto.AdminUserResponse;
import com.zhiyuan.college.model.entity.UserAccount;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface UserAccountMapper extends BaseMapper<UserAccount> {

    @Select("SELECT id, username, password, score, subject_type AS subject_type_value, exam_province, role AS role_value, enabled, created_at, updated_at FROM users WHERE username = #{username} LIMIT 1")
    UserAccount findByUsername(@Param("username") String username);

    @Select("SELECT id, username, password, score, subject_type AS subject_type_value, exam_province, role AS role_value, enabled, created_at, updated_at FROM users WHERE id = #{id} LIMIT 1")
    UserAccount findByIdCompat(@Param("id") Long id);

    @Select("SELECT role FROM users WHERE username = #{username} LIMIT 1")
    String findRoleByUsername(@Param("username") String username);

    @Select("""
            <script>
            SELECT u.id,
                   u.username,
                   u.score,
                   u.subject_type AS subjectType,
                   u.exam_province AS examProvince,
                   u.role,
                   u.enabled,
                   u.created_at AS createdAt,
                   u.updated_at AS updatedAt,
                   (SELECT COUNT(*) FROM recommendation_log r WHERE r.user_id = u.id) AS recommendationCount,
                   (SELECT COUNT(*) FROM application_plan p WHERE p.user_id = u.id) AS planCount,
                   (SELECT COUNT(*) FROM agent_conversation c WHERE c.user_id = u.id) AS conversationCount
            FROM users u
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND LOWER(u.username) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            </if>
            <if test="role != null and role != ''">
              AND u.role = #{role}
            </if>
            <if test="enabled != null">
              AND u.enabled = #{enabled}
            </if>
            ORDER BY u.created_at DESC, u.id DESC
            LIMIT #{limit} OFFSET #{offset}
            </script>
            """)
    List<AdminUserResponse> findAdminUsers(@Param("keyword") String keyword,
                                           @Param("role") String role,
                                           @Param("enabled") Boolean enabled,
                                           @Param("limit") int limit,
                                           @Param("offset") int offset);

    @Select("""
            <script>
            SELECT COUNT(*)
            FROM users u
            WHERE 1 = 1
            <if test="keyword != null and keyword != ''">
              AND LOWER(u.username) LIKE CONCAT('%', LOWER(#{keyword}), '%')
            </if>
            <if test="role != null and role != ''">
              AND u.role = #{role}
            </if>
            <if test="enabled != null">
              AND u.enabled = #{enabled}
            </if>
            </script>
            """)
    long countAdminUsers(@Param("keyword") String keyword,
                         @Param("role") String role,
                         @Param("enabled") Boolean enabled);

    @Select("""
            SELECT u.id,
                   u.username,
                   u.score,
                   u.subject_type AS subjectType,
                   u.exam_province AS examProvince,
                   u.role,
                   u.enabled,
                   u.created_at AS createdAt,
                   u.updated_at AS updatedAt,
                   (SELECT COUNT(*) FROM recommendation_log r WHERE r.user_id = u.id) AS recommendationCount,
                   (SELECT COUNT(*) FROM application_plan p WHERE p.user_id = u.id) AS planCount,
                   (SELECT COUNT(*) FROM agent_conversation c WHERE c.user_id = u.id) AS conversationCount
            FROM users u
            WHERE u.id = #{id}
            LIMIT 1
            """)
    AdminUserResponse findAdminUserById(@Param("id") Long id);

    @Select("""
            SELECT COUNT(*) AS totalCount,
                   SUM(CASE WHEN role = 'USER' THEN 1 ELSE 0 END) AS userCount,
                   SUM(CASE WHEN role = 'ADMIN' THEN 1 ELSE 0 END) AS adminCount,
                   SUM(CASE WHEN enabled = FALSE THEN 1 ELSE 0 END) AS disabledCount
            FROM users
            """)
    AdminUserOverviewResponse findAdminUserOverview();
}
