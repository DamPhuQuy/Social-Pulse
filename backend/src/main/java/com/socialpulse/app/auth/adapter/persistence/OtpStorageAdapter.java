package com.socialpulse.app.auth.adapter.persistence;
import org.springframework.stereotype.Repository;

import java.time.Duration;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.auth.domain.repository.OtpRepository;

@Service
@Repository
public class OtpStorageAdapter implements OtpRepository {

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


