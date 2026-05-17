package com.socialpulse.app.chat.infrastructure.websocket;

import java.util.Collections;
import java.util.Set;

import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import com.socialpulse.app.chat.domain.exception.MaxSessionsExceededException;

/**
 * Manages WebSocket session state in Redis for horizontal scaling support.
 *
 * <p>Uses two Redis key patterns:
 * <ul>
 *   <li>{@code ws:sessions:{userId}} — Set of active session IDs for a user</li>
 *   <li>{@code ws:session:{sessionId}} — Maps session ID to user ID</li>
 * </ul>
 *
 * <p>Both keys are cleaned up on disconnect (no TTL).
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

    /**
     * Registers a new WebSocket session for the given user.
     *
     * @param userId    the ID of the authenticated user
     * @param sessionId the WebSocket session ID
     * @throws MaxSessionsExceededException if the user already has 5 active sessions
     */
    public void registerSession(Long userId, String sessionId) {
        String sessionsKey = buildSessionsKey(userId);
        SetOperations<String, String> setOps = redisTemplate.opsForSet();

        Long currentCount = setOps.size(sessionsKey);
        if (currentCount != null && currentCount >= MAX_SESSIONS_PER_USER) {
            throw new MaxSessionsExceededException(userId);
        }

        setOps.add(sessionsKey, sessionId);

        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        valueOps.set(buildSessionKey(sessionId), userId.toString());
    }

    /**
     * Removes a WebSocket session, cleaning up both the session mapping
     * and the user's active sessions set.
     *
     * @param sessionId the WebSocket session ID to remove
     */
    public void removeSession(String sessionId) {
        ValueOperations<String, String> valueOps = redisTemplate.opsForValue();
        String userIdStr = valueOps.get(buildSessionKey(sessionId));

        if (userIdStr == null) {
            return;
        }

        String sessionsKey = buildSessionsKey(Long.valueOf(userIdStr));
        redisTemplate.opsForSet().remove(sessionsKey, sessionId);
        redisTemplate.delete(buildSessionKey(sessionId));
    }

    /**
     * Returns the set of active session IDs for the given user.
     *
     * @param userId the user ID
     * @return set of active session IDs, or empty set if none
     */
    public Set<String> getActiveSessions(Long userId) {
        Set<String> sessions = redisTemplate.opsForSet().members(buildSessionsKey(userId));
        return sessions != null ? sessions : Collections.emptySet();
    }

    /**
     * Checks whether the user has at least one active WebSocket session.
     *
     * @param userId the user ID
     * @return true if the user has one or more active sessions
     */
    public boolean isUserOnline(Long userId) {
        Long size = redisTemplate.opsForSet().size(buildSessionsKey(userId));
        return size != null && size > 0;
    }

    /**
     * Returns the number of active WebSocket sessions for the given user.
     *
     * @param userId the user ID
     * @return the count of active sessions
     */
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
