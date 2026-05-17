package com.socialpulse.app.chat.infrastructure.websocket;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.SetOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.socialpulse.app.chat.domain.exception.MaxSessionsExceededException;

@ExtendWith(MockitoExtension.class)
class WebSocketSessionManagerTest {

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SetOperations<String, String> setOperations;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private WebSocketSessionManager sessionManager;

    @BeforeEach
    void setUp() {
        sessionManager = new WebSocketSessionManager(redisTemplate);
    }

    @Test
    void registerSession_addsSessionToSetAndStoresMapping() {
        Long userId = 1L;
        String sessionId = "session-abc";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(2L);

        sessionManager.registerSession(userId, sessionId);

        verify(setOperations).add("ws:sessions:1", "session-abc");
        verify(valueOperations).set("ws:session:session-abc", "1");
    }

    @Test
    void registerSession_throwsWhenMaxSessionsReached() {
        Long userId = 1L;
        String sessionId = "session-new";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(5L);

        assertThrows(MaxSessionsExceededException.class,
                () -> sessionManager.registerSession(userId, sessionId));
    }

    @Test
    void registerSession_allowsWhenBelowMaxSessions() {
        Long userId = 1L;
        String sessionId = "session-4th";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(4L);

        sessionManager.registerSession(userId, sessionId);

        verify(setOperations).add("ws:sessions:1", "session-4th");
        verify(valueOperations).set("ws:session:session-4th", "1");
    }

    @Test
    void registerSession_allowsFirstSession() {
        Long userId = 1L;
        String sessionId = "session-first";

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(0L);

        sessionManager.registerSession(userId, sessionId);

        verify(setOperations).add("ws:sessions:1", "session-first");
        verify(valueOperations).set("ws:session:session-first", "1");
    }

    @Test
    void removeSession_removesFromBothKeys() {
        String sessionId = "session-abc";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:session:session-abc")).thenReturn("1");
        when(redisTemplate.opsForSet()).thenReturn(setOperations);

        sessionManager.removeSession(sessionId);

        verify(setOperations).remove("ws:sessions:1", "session-abc");
        verify(redisTemplate).delete("ws:session:session-abc");
    }

    @Test
    void removeSession_doesNothingWhenSessionNotFound() {
        String sessionId = "session-unknown";

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("ws:session:session-unknown")).thenReturn(null);

        sessionManager.removeSession(sessionId);

        verify(redisTemplate, never()).opsForSet();
        verify(redisTemplate, never()).delete("ws:session:session-unknown");
    }

    @Test
    void getActiveSessions_returnsSessionSet() {
        Long userId = 1L;
        Set<String> expected = Set.of("session-1", "session-2");

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("ws:sessions:1")).thenReturn(expected);

        Set<String> result = sessionManager.getActiveSessions(userId);

        assertEquals(expected, result);
    }

    @Test
    void getActiveSessions_returnsEmptySetWhenNull() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.members("ws:sessions:1")).thenReturn(null);

        Set<String> result = sessionManager.getActiveSessions(userId);

        assertEquals(Collections.emptySet(), result);
    }

    @Test
    void isUserOnline_returnsTrueWhenSessionsExist() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(2L);

        assertTrue(sessionManager.isUserOnline(userId));
    }

    @Test
    void isUserOnline_returnsFalseWhenNoSessions() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(0L);

        assertFalse(sessionManager.isUserOnline(userId));
    }

    @Test
    void isUserOnline_returnsFalseWhenNull() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(null);

        assertFalse(sessionManager.isUserOnline(userId));
    }

    @Test
    void getSessionCount_returnsCorrectCount() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(3L);

        assertEquals(3, sessionManager.getSessionCount(userId));
    }

    @Test
    void getSessionCount_returnsZeroWhenNull() {
        Long userId = 1L;

        when(redisTemplate.opsForSet()).thenReturn(setOperations);
        when(setOperations.size("ws:sessions:1")).thenReturn(null);

        assertEquals(0, sessionManager.getSessionCount(userId));
    }
}
