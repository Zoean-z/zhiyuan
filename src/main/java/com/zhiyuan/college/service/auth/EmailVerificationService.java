package com.zhiyuan.college.service.auth;

import com.zhiyuan.college.mapper.EmailVerificationCodeMapper;
import com.zhiyuan.college.mapper.UserAccountMapper;
import com.zhiyuan.college.model.dto.EmailVerificationCodeResponse;
import com.zhiyuan.college.model.entity.EmailVerificationCode;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class EmailVerificationService {

    static final String PURPOSE_REGISTER = "REGISTER";
    static final int CODE_TTL_SECONDS = 600;
    static final int RESEND_COOLDOWN_SECONDS = 60;
    static final int MAX_ATTEMPTS = 5;

    private final EmailVerificationCodeMapper codeMapper;
    private final UserAccountMapper userAccountMapper;
    private final PasswordEncoder passwordEncoder;
    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final SecureRandom secureRandom = new SecureRandom();
    private final String mailFrom;

    public EmailVerificationService(EmailVerificationCodeMapper codeMapper,
                                    UserAccountMapper userAccountMapper,
                                    PasswordEncoder passwordEncoder,
                                    ObjectProvider<JavaMailSender> mailSenderProvider,
                                    @Value("${app.mail.from:${spring.mail.username:}}") String mailFrom) {
        this.codeMapper = codeMapper;
        this.userAccountMapper = userAccountMapper;
        this.passwordEncoder = passwordEncoder;
        this.mailSenderProvider = mailSenderProvider;
        this.mailFrom = mailFrom == null ? "" : mailFrom.trim();
    }

    @Transactional
    public EmailVerificationCodeResponse requestRegistrationCode(String rawEmail) {
        String email = normalizeEmail(rawEmail);
        if (userAccountMapper.findByEmail(email) != null) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "该邮箱已注册");
        }

        LocalDateTime now = LocalDateTime.now();
        EmailVerificationCode latest = codeMapper.findLatest(email, PURPOSE_REGISTER);
        if (latest != null && latest.getRequestedAt() != null) {
            long elapsed = Duration.between(latest.getRequestedAt(), now).getSeconds();
            if (elapsed >= 0 && elapsed < RESEND_COOLDOWN_SECONDS) {
                throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS,
                        "验证码发送过于频繁，请 " + (RESEND_COOLDOWN_SECONDS - elapsed) + " 秒后重试");
            }
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null || mailFrom.isBlank()) {
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "邮件服务尚未配置");
        }

        String code = String.format(Locale.ROOT, "%06d", secureRandom.nextInt(1_000_000));
        codeMapper.invalidateActive(email, PURPOSE_REGISTER);
        EmailVerificationCode record = new EmailVerificationCode();
        record.setEmail(email);
        record.setCodeHash(passwordEncoder.encode(code));
        record.setPurpose(PURPOSE_REGISTER);
        record.setExpiresAt(now.plusSeconds(CODE_TTL_SECONDS));
        record.setAttemptCount(0);
        record.setConsumed(false);
        record.setRequestedAt(now);
        codeMapper.insert(record);

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(mailFrom);
        message.setTo(email);
        message.setSubject("智愿AI报考平台注册验证码");
        message.setText("你的注册验证码是：" + code + "\n\n验证码 10 分钟内有效，请勿转发给他人。");
        try {
            mailSender.send(message);
        } catch (MailException ex) {
            codeMapper.consume(record.getId());
            throw new ResponseStatusException(HttpStatus.SERVICE_UNAVAILABLE, "验证码邮件发送失败，请稍后重试");
        }

        return new EmailVerificationCodeResponse("验证码已发送", CODE_TTL_SECONDS, RESEND_COOLDOWN_SECONDS);
    }

    @Transactional(noRollbackFor = ResponseStatusException.class)
    public String verifyAndConsumeRegistrationCode(String rawEmail, String rawCode) {
        String email = normalizeEmail(rawEmail);
        String code = rawCode == null ? "" : rawCode.trim();
        EmailVerificationCode record = codeMapper.findLatestActiveForUpdate(email, PURPOSE_REGISTER);
        LocalDateTime now = LocalDateTime.now();
        if (record == null || record.getExpiresAt() == null || !record.getExpiresAt().isAfter(now)) {
            if (record != null) codeMapper.consume(record.getId());
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码无效或已过期");
        }
        if (record.getAttemptCount() != null && record.getAttemptCount() >= MAX_ATTEMPTS) {
            codeMapper.consume(record.getId());
            throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, "验证码错误次数过多，请重新获取");
        }
        if (!passwordEncoder.matches(code, record.getCodeHash())) {
            codeMapper.recordFailedAttempt(record.getId(), MAX_ATTEMPTS);
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "验证码无效或已过期");
        }
        if (codeMapper.consume(record.getId()) != 1) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "验证码已被使用");
        }
        return email;
    }

    static String normalizeEmail(String email) {
        return email == null ? "" : email.trim().toLowerCase(Locale.ROOT);
    }
}
