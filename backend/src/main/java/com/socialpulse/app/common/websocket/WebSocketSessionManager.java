package com.socialpulse.app.common.websocket;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

/**
 * Manages WebSocket session state in Redis for horizontal scaling support.
 *
 * <p>Uses two Redis key patterns:
 * <ul>
 *   <li>{@code ws:sessions:{userId}} — Set of active session IDs for a user</li>
 *   <li>{@code ws:session:{sessionId}} — Maps session ID to user ID</li>
 * </ul>
 */
@Component
public class WebSocketSessionManager {

    private static final String SESSIONS_KEY_PREFIX = "ws:sessions:";
    private static final String SESSION_KEY_PREFIX = "ws:session:";
    private static final int MAX_SESSIONS_PER_USER = 5;

    private final StringRedisTemplate redisTemplate;

    public WebSocketSessionManager(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public void registerSession(Long userId, String sessionId) {
        String sessionsKey = buildSessionsKey(userId);
        Long currentCount = redisTemplate.opsForSet().size(sessionsKey);
        if (currentCount != null && currentCount >= MAX_SESSIONS_PER_USER) {
            throw new MaxWebSocketSessionsException(userId);
        }
        redisTemplate.opsForSet().add(sessionsKey, sessionId);
        redisTemplate.opsForValue().set(buildSessionKey(sessionId), userId.toString());
    }

    public void removeSession(String sessionId) {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        String userIdStr = valueOps.get(buildSessionKey(sessionId));
        if (userIdStr == null) {
            return;
        }
        redisTemplate.opsForSet().remove(buildSessionsKey(Long.valueOf(userIdStr)), sessionId);
        redisTemplate.delete(buildSessionKey(sessionId));
    }

    public Set<String> getActiveSessions(Long userId) {
        Set<String> sessions = redisTemplate.opsForSet().members(buildSessionsKey(userId));
        return sessions != null ? sessions : Collections.emptySet();
    }

    public boolean isUserOnline(Long userId) {
        Long size = redisTemplate.opsForSet().size(buildSessionsKey(userId));
        return size != null && size > 0;
    }

    public int getSessionCount(Long userId) {
        Long size = redisTemplate.opsForSet().size(buildSessionsKey(userId));
        return size != null ? size.intValue() : 0;
    }

    private String buildSessionsKey(Long userId) {
        return SESSIONS_KEY_PREFIX + userId;
    }

    private String buildSessionKey(String sessionId) {
        return SESSION_KEY_PREFIX + sessionId;
    }
}
