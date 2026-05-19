package com.socialpulse.app.common.websocket;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.messaging.SessionConnectedEvent;
import org.springframework.web.socket.messaging.SessionDisconnectEvent;

import com.socialpulse.app.security.user.CustomUserDetails;

/**
 * Listens to WebSocket session lifecycle events and manages session registration.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketSessionManager sessionManager;

    public WebSocketEventListener(WebSocketSessionManager sessionManager) {
        this.sessionManager = sessionManager;
    }

    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        CustomUserDetails userDetails = extractUserDetails(accessor);
        if (userDetails == null) {
            log.warn("WebSocket connected event received without authentication for session: {}", sessionId);
            return;
        }

        sessionManager.registerSession(userDetails.getId(), sessionId);
        log.info("WebSocket session connected: userId={}, sessionId={}", userDetails.getId(), sessionId);
    }

    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }

        CustomUserDetails userDetails = extractUserDetails(accessor);
        if (userDetails != null) {
            boolean stillOnline = sessionManager.isUserOnline(userDetails.getId());
            log.info("WebSocket session disconnected: userId={}, sessionId={}, stillOnline={}",
                    userDetails.getId(), sessionId, stillOnline);
        }
    }

    private CustomUserDetails extractUserDetails(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken
                && authToken.getPrincipal() instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
