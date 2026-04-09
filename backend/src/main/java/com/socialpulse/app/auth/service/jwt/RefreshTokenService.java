package com.socialpulse.app.auth.service.jwt;

import com.socialpulse.app.auth.security.jwt.JwtProperties;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.UUID;

@Service
public class RefreshTokenService {

    private static final String REFRESH_TOKEN_PREFIX = "auth:refresh:";

    private final StringRedisTemplate redisTemplate;
    private final JwtProperties jwtProperties;

    public RefreshTokenService(StringRedisTemplate redisTemplate, JwtProperties jwtProperties) {
        this.redisTemplate = redisTemplate;
        this.jwtProperties = jwtProperties;
    }

    /**
     * Generate a new refresh token and store it in Redis
     * @param email The user's email
     * @return The generated refresh token string
     */
    public String generateRefreshToken(String email) {
        String refreshToken = UUID.randomUUID().toString();
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        
        long durationMs = jwtProperties.getRefreshExpirationMs();
        redisTemplate.opsForValue().set(key, email, Duration.ofMillis(durationMs));
        
        return refreshToken;
    }

    /**
     * Verify the refresh token and return the associated email
     * @param refreshToken The refresh token string
     * @return The user's email if valid, or null if invalid/expired
     */
    public String verifyRefreshToken(String refreshToken) {
        if (refreshToken == null || refreshToken.trim().isEmpty()) {
            return null;
        }
        String key = REFRESH_TOKEN_PREFIX + refreshToken;
        return redisTemplate.opsForValue().get(key);
    }

    /**
     * Invalidates a specific refresh token (e.g. on logout)
     * @param refreshToken The refresh token to invalidate
     */
    public void invalidateRefreshToken(String refreshToken) {
        if (refreshToken != null && !refreshToken.trim().isEmpty()) {
            String key = REFRESH_TOKEN_PREFIX + refreshToken;
            redisTemplate.delete(key);
        }
    }
}
