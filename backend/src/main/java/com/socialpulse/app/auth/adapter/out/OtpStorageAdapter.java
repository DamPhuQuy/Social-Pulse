package com.socialpulse.app.auth.adapter.out;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.auth.application.port.out.OtpStoragePort;

@Service
public class OtpStorageAdapter implements OtpStoragePort {

    private static final String OTP_KEY_PREFIX = "auth:otp:";
    private static final long OTP_TTL_SECONDS = 300;

    private final StringRedisTemplate redisTemplate;

    public OtpStorageAdapter(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    @Override
    public void save(String email, String code) {
        redisTemplate.opsForValue().set(buildOtpKey(email), code, Duration.ofSeconds(OTP_TTL_SECONDS));
    }

    @Override
    public String findByEmail(String email) {
        return redisTemplate.opsForValue().get(buildOtpKey(email));
    }

    @Override
    public void delete(String email) {
        redisTemplate.delete(buildOtpKey(email));
    }

    private String buildOtpKey(String email) {
        return OTP_KEY_PREFIX + email;
    }
}
