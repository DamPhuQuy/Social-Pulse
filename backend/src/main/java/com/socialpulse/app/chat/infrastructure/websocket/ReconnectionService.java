package com.socialpulse.app.chat.infrastructure.websocket;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.stereotype.Service;
import org.springframework.web.socket.messaging.SessionConnectedEvent;

import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

import jakarta.annotation.PreDestroy;

/**
 * Handles delivery of unread counts and pending status updates when a user
 * reconnects via WebSocket.
 *
 * <p>After a session is registered, this service schedules delivery with a 500ms
 * delay to ensure the client has subscribed to the relevant queues. All deliveries
 * complete within 2 seconds of session registration per requirements 4.5 and 7.3.
 *
 * <p>Unread counts are read from Redis keys {@code chat:unread:{conversationId}:{userId}}.
 * Pending status updates are popped from Redis list {@code chat:pending-status:{userId}}.
 */
@Service
public class ReconnectionService {

    private static final Logger log = LoggerFactory.getLogger(ReconnectionService.class);

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final String PENDING_STATUS_KEY_PREFIX = "chat:pending-status:";
    private static final long DELIVERY_DELAY_MS = 500;
    private static final int MAX_CONVERSATIONS_PAGE_SIZE = 100;

    private final ConversationRepository conversationRepository;
    private final StringRedisTemplate redisTemplate;
    private final SimpMessagingTemplate messagingTemplate;
    private final ScheduledExecutorService scheduler;

    public ReconnectionService(
            ConversationRepository conversationRepository,
            StringRedisTemplate redisTemplate,
            SimpMessagingTemplate messagingTemplate) {
        this.conversationRepository = conversationRepository;
        this.redisTemplate = redisTemplate;
        this.messagingTemplate = messagingTemplate;
        this.scheduler = new ScheduledThreadPoolExecutor(2);
    }

    /**
     * Listens for WebSocket session connections and triggers reconnection delivery.
     */
    @EventListener
    public void onSessionConnected(SessionConnectedEvent event) {
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(event.getMessage());
        if (accessor.getUser() instanceof UsernamePasswordAuthenticationToken authToken
                && authToken.getPrincipal() instanceof CustomUserDetails userDetails) {
            scheduleReconnectionDelivery(userDetails.getId(), userDetails.getUsername());
        }
    }

    /**
     * Schedules delivery of unread counts and pending status updates for the user.
     * Called after a WebSocket session is registered.
     *
     * @param userId   the ID of the reconnecting user
     * @param username the username (email) used for STOMP user destination routing
     */
    public void scheduleReconnectionDelivery(Long userId, String username) {
        scheduler.schedule(() -> deliverReconnectionData(userId, username),
                DELIVERY_DELAY_MS, TimeUnit.MILLISECONDS);
    }

    /**
     * Delivers unread counts and pending status updates to the user.
     */
    private void deliverReconnectionData(Long userId, String username) {
        try {
            deliverUnreadCounts(userId, username);
            deliverPendingStatusUpdates(userId, username);
        } catch (Exception e) {
            log.error("Failed to deliver reconnection data for userId={}: {}",
                    userId, e.getMessage(), e);
        }
    }

    /**
     * Queries all conversations for the user and delivers unread counts > 0.
     */
    private void deliverUnreadCounts(Long userId, String username) {
        List<Conversation> conversations = conversationRepository.findByUserId(userId, 0, MAX_CONVERSATIONS_PAGE_SIZE);

        List<UnreadCountEntry> unreadEntries = new ArrayList<>();
        for (Conversation conversation : conversations) {
            String unreadKey = UNREAD_KEY_PREFIX + conversation.getId() + ":" + userId;
            String countStr = redisTemplate.opsForValue().get(unreadKey);

            if (countStr != null) {
                int count = Integer.parseInt(countStr);
                if (count > 0) {
                    unreadEntries.add(new UnreadCountEntry(conversation.getId(), count));
                }
            }
        }

        if (!unreadEntries.isEmpty()) {
            messagingTemplate.convertAndSendToUser(username, "/queue/unread-counts", unreadEntries);
            log.debug("Delivered {} unread count entries to user {} (userId={})",
                    unreadEntries.size(), username, userId);
        }
    }

    /**
     * Pops all pending status updates from Redis and delivers them to the user.
     */
    private void deliverPendingStatusUpdates(Long userId, String username) {
        String pendingKey = PENDING_STATUS_KEY_PREFIX + userId;

        List<String> pendingUpdates = new ArrayList<>();
        String entry;
        while ((entry = redisTemplate.opsForList().leftPop(pendingKey)) != null) {
            pendingUpdates.add(entry);
        }

        if (!pendingUpdates.isEmpty()) {
            for (String update : pendingUpdates) {
                messagingTemplate.convertAndSendToUser(username, "/queue/status-updates", update);
            }
            log.debug("Delivered {} pending status updates to user {} (userId={})",
                    pendingUpdates.size(), username, userId);
        }
    }

    @PreDestroy
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }

    /**
     * DTO representing an unread count entry for a conversation.
     */
    public record UnreadCountEntry(Long conversationId, int unreadCount) {}
}
