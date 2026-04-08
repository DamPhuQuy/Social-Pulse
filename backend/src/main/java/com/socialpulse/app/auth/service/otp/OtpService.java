package com.socialpulse.app.auth.service.otp;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import com.socialpulse.app.auth.service.email.EmailService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.auth.entity.Otp;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.status.ErrorCode;
import com.socialpulse.app.auth.security.encoder.PasswordEncoder;

@Service
public class OtpService {

    private static final String OTP_KEY_PREFIX = "auth:otp:";
    private static final long OTP_TTL_SECONDS = 300;
    private static final long OTP_MAX_ATTEMPTS = 5;

    private final StringRedisTemplate redisTemplate;
    private final SecureRandom secureRandom;
    private final Logger logger;
    private final EmailService emailService;
    PasswordEncoder passwordEncoder;

    public OtpService(StringRedisTemplate redisTemplate, EmailService emailService, PasswordEncoder passwordEncoder) {
        this.redisTemplate = redisTemplate;
        this.emailService = emailService;
        this.passwordEncoder = passwordEncoder;
        this.secureRandom = new SecureRandom();
        this.logger = LoggerFactory.getLogger(OtpService.class);
    }

    public void generateToStoreAndSendEmail(String email) {
        String normalizedEmail = normalizeEmail(email);
        String otpCode = generateOtpCode();
        saveToRedis(normalizedEmail, otpCode);
        emailService.sendOtpEmail(normalizedEmail, otpCode);
    }

    public void saveToRedis(String email, String code) {
        String normalizedEmail = normalizeEmail(email);
        String key = buildOtpKey(normalizedEmail);

        Otp otp = Otp.builder()
                .email(normalizedEmail)
                .otpCode(passwordEncoder.encode(code))
                .expiredAt(Instant.now().plusSeconds(OTP_TTL_SECONDS).toEpochMilli())
                .attemptCount(0L)
                .build();

        redisTemplate.opsForHash().putAll(key, Map.of(
                "otpCode", otp.getOtpCode(),
                "expiredAt", String.valueOf(otp.getExpiredAt()),
                "attemptCount", String.valueOf(otp.getAttemptCount())));
        redisTemplate.expire(key, Duration.ofSeconds(OTP_TTL_SECONDS));
    }

    public void verifyOtp(String email, String otpCode) {
        String normalizedEmail = normalizeEmail(email);
        String key = buildOtpKey(normalizedEmail);
        Otp otp = readOtp(key, normalizedEmail);

        if (otp == null || otp.isExpired(Instant.now().toEpochMilli())) {
            redisTemplate.delete(key);
            throw new AppException(ErrorCode.OTP_EXPIRED);
        }

        if (otp.getAttemptCount() >= OTP_MAX_ATTEMPTS) {
            throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
        }

        if (!passwordEncoder.matches(otpCode.trim(), otp.getOtpCode())) {
            long updatedAttempts = otp.getAttemptCount() + 1;
            redisTemplate.opsForHash().put(key, "attemptCount", String.valueOf(updatedAttempts));

            Long remainingTtl = redisTemplate.getExpire(key, TimeUnit.SECONDS);
            if (remainingTtl != null && remainingTtl > 0) {
                redisTemplate.expire(key, Duration.ofSeconds(remainingTtl));
            }

            if (updatedAttempts >= OTP_MAX_ATTEMPTS) {
                throw new AppException(ErrorCode.OTP_TOO_MANY_ATTEMPTS);
            }
            throw new AppException(ErrorCode.OTP_INVALID);
        }
    }

    public void invalidateOtp(String email) {
        redisTemplate.delete(buildOtpKey(normalizeEmail(email)));
    }

    private Otp readOtp(String key, String email) {
        Map<Object, Object> data = redisTemplate.opsForHash().entries(key);
        if (data.isEmpty()) {
            return null;
        }

        return Otp.builder()
                .email(email)
                .otpCode((String) data.get("otpCode"))
                .expiredAt(parseLong(data.get("expiredAt")))
                .attemptCount(parseLong(data.get("attemptCount")))
                .build();
    }

    private long parseLong(Object value) {
        if (value == null) {
            return 0L;
        }
        return Long.parseLong(String.valueOf(value));
    }

    private String generateOtpCode() {
        int otp = secureRandom.nextInt(1_000_000);
        return String.format("%06d", otp);
    }

    private String buildOtpKey(String email) {
        return OTP_KEY_PREFIX + email;
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
