package com.socialpulse.app.chat.infrastructure.websocket;

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
 * Listens to WebSocket session lifecycle events (connect/disconnect) and
 * manages user session registration via the {@link WebSocketSessionManager}.
 *
 * <p>On connect: registers the session in Redis so the user is tracked as online,
 * then triggers reconnection delivery of unread counts and pending status updates.
 * <p>On disconnect (client-initiated or heartbeat timeout): removes the session
 * and updates the user's online status if no remaining sessions exist.
 *
 * <p>Heartbeat timeout (30 seconds) is handled automatically by Spring WebSocket —
 * when heartbeat fails, it triggers a {@link SessionDisconnectEvent}.
 */
@Component
public class WebSocketEventListener {

    private static final Logger log = LoggerFactory.getLogger(WebSocketEventListener.class);

    private final WebSocketSessionManager sessionManager;
    private final ReconnectionService reconnectionService;

    public WebSocketEventListener(WebSocketSessionManager sessionManager,
                                  ReconnectionService reconnectionService) {
        this.sessionManager = sessionManager;
        this.reconnectionService = reconnectionService;
    }

    /**
     * Handles a new WebSocket session connection.
     * Extracts the authenticated user principal and session ID from the event,
     * then registers the session via the session manager.
     */
    @EventListener
    public void handleSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        UsernamePasswordAuthenticationToken authToken = extractAuthToken(accessor);
        if (authToken == null) {
            log.warn("WebSocket connected event received without authentication for session: {}", sessionId);
            return;
        }

        CustomUserDetails userDetails = extractUserDetails(authToken);
        if (userDetails == null) {
            log.warn("WebSocket connected event: unable to extract user details for session: {}", sessionId);
            return;
        }

        Long userId = userDetails.getId();
        String username = userDetails.getUsername();
        sessionManager.registerSession(userId, sessionId);
        log.info("WebSocket session connected: userId={}, sessionId={}", userId, sessionId);

        // Trigger reconnection delivery of unread counts and pending status updates
        reconnectionService.scheduleReconnectionDelivery(userId, username);
    }

    /**
     * Handles a WebSocket session disconnect (client-initiated close or heartbeat timeout).
     * Removes the session from the session manager. If no remaining sessions exist
     * for the user, they are effectively marked as offline.
     */
    @EventListener
    public void handleSessionDisconnect(SessionDisconnectEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        String sessionId = accessor.getSessionId();

        UsernamePasswordAuthenticationToken authToken = extractAuthToken(accessor);
        if (authToken == null) {
            log.warn("WebSocket disconnect event received without authentication for session: {}", sessionId);
            // Still attempt to remove the session by sessionId in case it was registered
            if (sessionId != null) {
                sessionManager.removeSession(sessionId);
            }
            return;
        }

        CustomUserDetails userDetails = extractUserDetails(authToken);
        if (userDetails == null) {
            log.warn("WebSocket disconnect event: unable to extract user details for session: {}", sessionId);
            if (sessionId != null) {
                sessionManager.removeSession(sessionId);
            }
            return;
        }

        Long userId = userDetails.getId();

        if (sessionId != null) {
            sessionManager.removeSession(sessionId);
        }

        boolean stillOnline = sessionManager.isUserOnline(userId);
        log.info("WebSocket session disconnected: userId={}, sessionId={}, stillOnline={}",
                userId, sessionId, stillOnline);
    }

    /**
     * Extracts the {@link UsernamePasswordAuthenticationToken} from the STOMP header accessor.
     * The token is set by {@link WebSocketAuthInterceptor} during the CONNECT frame.
     */
    private UsernamePasswordAuthenticationToken extractAuthToken(StompHeaderAccessor accessor) {
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken) {
            return authToken;
        }
        return null;
    }

    /**
     * Extracts {@link CustomUserDetails} from the authentication token's principal.
     */
    private CustomUserDetails extractUserDetails(UsernamePasswordAuthenticationToken authToken) {
        Object principal = authToken.getPrincipal();
        if (principal instanceof CustomUserDetails userDetails) {
            return userDetails;
        }
        return null;
    }
}
