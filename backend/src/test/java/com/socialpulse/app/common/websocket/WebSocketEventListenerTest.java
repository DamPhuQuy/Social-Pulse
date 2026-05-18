package com.socialpulse.app.common.websocket;

import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageHeaders;
import org.springframework.messaging.support.GenericMessage;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.socialpulse.app.security.user.CustomUserDetails;

@ExtendWith(MockitoExtension.class)
class WebSocketEventListenerTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private CustomUserDetails customUserDetails;

    private WebSocketEventListener eventListener;

    @BeforeEach
    void setUp() {
        eventListener = new WebSocketEventListener(sessionManager);
    }

    @Test
    void handleSessionConnected_registersSessionViaSessionManager() {
        Long userId = 42L;
        String sessionId = "session-123";

        when(customUserDetails.getId()).thenReturn(userId);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, List.of());

        Message<byte[]> message = createMessageWithUserAndSession(authToken, sessionId);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message);

        eventListener.handleSessionConnected(event);

        verify(sessionManager).registerSession(userId, sessionId);
    }

    @Test
    void handleSessionConnected_doesNothingWhenNoAuthentication() {
        String sessionId = "session-456";

        Message<byte[]> message = createMessageWithUserAndSession(null, sessionId);
        SessionConnectedEvent event = new SessionConnectedEvent(this, message);

        eventListener.handleSessionConnected(event);

        verify(sessionManager, never()).registerSession(org.mockito.ArgumentMatchers.anyLong(),
                org.mockito.ArgumentMatchers.anyString());
    }

    @Test
    void handleSessionDisconnect_removesSession() {
        Long userId = 42L;
        String sessionId = "session-123";

        when(customUserDetails.getId()).thenReturn(userId);
        when(sessionManager.isUserOnline(userId)).thenReturn(false);

        UsernamePasswordAuthenticationToken authToken =
                new UsernamePasswordAuthenticationToken(customUserDetails, null, List.of());

        Message<byte[]> message = createMessageWithUserAndSession(authToken, sessionId);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, null);

        eventListener.handleSessionDisconnect(event);

        verify(sessionManager).removeSession(sessionId);
    }

    @Test
    void handleSessionDisconnect_removesSessionEvenWithoutAuthentication() {
        String sessionId = "session-789";

        Message<byte[]> message = createMessageWithUserAndSession(null, sessionId);
        SessionDisconnectEvent event = new SessionDisconnectEvent(this, message, sessionId, null);

        eventListener.handleSessionDisconnect(event);

        verify(sessionManager).removeSession(sessionId);
    }

    private Message<byte[]> createMessageWithUserAndSession(
            UsernamePasswordAuthenticationToken authToken, String sessionId) {
        Map<String, Object> headers = new java.util.HashMap<>();
        headers.put("simpSessionId", sessionId);
        if (authToken != null) {
            headers.put("simpUser", authToken);
        }
        return new GenericMessage<>(new byte[0], new MessageHeaders(headers));
    }
}
