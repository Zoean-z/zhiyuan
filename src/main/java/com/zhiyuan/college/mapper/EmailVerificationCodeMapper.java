package com.zhiyuan.college.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.zhiyuan.college.model.entity.EmailVerificationCode;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface EmailVerificationCodeMapper extends BaseMapper<EmailVerificationCode> {

    @Select("SELECT id, email, code_hash, purpose, expires_at, attempt_count, consumed, requested_at, consumed_at "
            + "FROM email_verification_code WHERE email = #{email} AND purpose = #{purpose} "
            + "ORDER BY id DESC LIMIT 1")
    EmailVerificationCode findLatest(@Param("email") String email, @Param("purpose") String purpose);

    @Select("SELECT id, email, code_hash, purpose, expires_at, attempt_count, consumed, requested_at, consumed_at "
            + "FROM email_verification_code WHERE email = #{email} AND purpose = #{purpose} AND consumed = FALSE "
            + "ORDER BY id DESC LIMIT 1 FOR UPDATE")
    EmailVerificationCode findLatestActiveForUpdate(@Param("email") String email, @Param("purpose") String purpose);

    @Update("UPDATE email_verification_code SET consumed = TRUE, consumed_at = CURRENT_TIMESTAMP "
            + "WHERE email = #{email} AND purpose = #{purpose} AND consumed = FALSE")
    int invalidateActive(@Param("email") String email, @Param("purpose") String purpose);

    @Update("UPDATE email_verification_code SET attempt_count = attempt_count + 1, "
            + "consumed = CASE WHEN attempt_count + 1 >= #{maxAttempts} THEN TRUE ELSE consumed END, "
            + "consumed_at = CASE WHEN attempt_count + 1 >= #{maxAttempts} THEN CURRENT_TIMESTAMP ELSE consumed_at END "
            + "WHERE id = #{id} AND consumed = FALSE")
    int recordFailedAttempt(@Param("id") Long id, @Param("maxAttempts") int maxAttempts);

    @Update("UPDATE email_verification_code SET consumed = TRUE, consumed_at = CURRENT_TIMESTAMP "
            + "WHERE id = #{id} AND consumed = FALSE")
    int consume(@Param("id") Long id);
}
