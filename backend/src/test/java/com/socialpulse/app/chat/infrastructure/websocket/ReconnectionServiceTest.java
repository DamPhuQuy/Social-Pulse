package com.socialpulse.app.chat.infrastructure.websocket;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.ListOperations;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;

@ExtendWith(MockitoExtension.class)
class ReconnectionServiceTest {

    @Mock
    private ConversationRepository conversationRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    @Mock
    private ListOperations<String, String> listOperations;

    private ReconnectionService reconnectionService;

    @BeforeEach
    void setUp() {
        reconnectionService = new ReconnectionService(
                conversationRepository, redisTemplate, messagingTemplate);
    }

    @AfterEach
    void tearDown() {
        reconnectionService.shutdown();
    }

    @Test
    void scheduleReconnectionDelivery_deliversUnreadCountsForConversationsWithUnread() {
        Long userId = 1L;
        String username = "user@example.com";
        Long conversationId = 10L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participant1Id(userId)
                .participant2Id(2L)
                .createdAt(Instant.now())
                .build();

        when(conversationRepository.findByUserId(userId, 0, 100))
                .thenReturn(List.of(conversation));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:unread:" + conversationId + ":" + userId))
                .thenReturn("3");
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("chat:pending-status:" + userId))
                .thenReturn(null);

        reconnectionService.scheduleReconnectionDelivery(userId, username);

        // Verify delivery happens within 2 seconds (500ms delay + processing)
        verify(messagingTemplate, timeout(2000))
                .convertAndSendToUser(eq(username), eq("/queue/unread-counts"), any());
    }

    @Test
    void scheduleReconnectionDelivery_doesNotDeliverWhenNoUnreadCounts() {
        Long userId = 1L;
        String username = "user@example.com";
        Long conversationId = 10L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participant1Id(userId)
                .participant2Id(2L)
                .createdAt(Instant.now())
                .build();

        when(conversationRepository.findByUserId(userId, 0, 100))
                .thenReturn(List.of(conversation));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:unread:" + conversationId + ":" + userId))
                .thenReturn("0");
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("chat:pending-status:" + userId))
                .thenReturn(null);

        reconnectionService.scheduleReconnectionDelivery(userId, username);

        // Wait for the scheduled task to complete
        verify(redisTemplate, timeout(2000).atLeastOnce()).opsForList();
        verify(messagingTemplate, never())
                .convertAndSendToUser(eq(username), eq("/queue/unread-counts"), any());
    }

    @Test
    void scheduleReconnectionDelivery_deliversPendingStatusUpdates() {
        Long userId = 1L;
        String username = "user@example.com";

        String statusUpdate1 = "{\"messageId\":1,\"conversationId\":10,\"previousStatus\":\"SENT\",\"newStatus\":\"DELIVERED\",\"updatedAt\":\"2024-01-01T00:00:00Z\"}";
        String statusUpdate2 = "{\"messageId\":2,\"conversationId\":10,\"previousStatus\":\"DELIVERED\",\"newStatus\":\"READ\",\"updatedAt\":\"2024-01-01T00:01:00Z\"}";

        when(conversationRepository.findByUserId(userId, 0, 100))
                .thenReturn(List.of());
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("chat:pending-status:" + userId))
                .thenReturn(statusUpdate1)
                .thenReturn(statusUpdate2)
                .thenReturn(null);

        reconnectionService.scheduleReconnectionDelivery(userId, username);

        // Verify both status updates are delivered
        verify(messagingTemplate, timeout(2000))
                .convertAndSendToUser(username, "/queue/status-updates", statusUpdate1);
        verify(messagingTemplate, timeout(2000))
                .convertAndSendToUser(username, "/queue/status-updates", statusUpdate2);
    }

    @Test
    void scheduleReconnectionDelivery_handlesNoConversationsGracefully() {
        Long userId = 1L;
        String username = "user@example.com";

        when(conversationRepository.findByUserId(userId, 0, 100))
                .thenReturn(List.of());
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("chat:pending-status:" + userId))
                .thenReturn(null);

        reconnectionService.scheduleReconnectionDelivery(userId, username);

        // Wait for the scheduled task to complete
        verify(redisTemplate, timeout(2000).atLeastOnce()).opsForList();
        verify(messagingTemplate, never())
                .convertAndSendToUser(any(), any(), any());
    }

    @Test
    void scheduleReconnectionDelivery_deliversWithin2SecondsOfRegistration() {
        Long userId = 1L;
        String username = "user@example.com";
        Long conversationId = 10L;

        Conversation conversation = Conversation.builder()
                .id(conversationId)
                .participant1Id(userId)
                .participant2Id(2L)
                .createdAt(Instant.now())
                .build();

        when(conversationRepository.findByUserId(userId, 0, 100))
                .thenReturn(List.of(conversation));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.get("chat:unread:" + conversationId + ":" + userId))
                .thenReturn("5");
        when(redisTemplate.opsForList()).thenReturn(listOperations);
        when(listOperations.leftPop("chat:pending-status:" + userId))
                .thenReturn(null);

        long startTime = System.currentTimeMillis();
        reconnectionService.scheduleReconnectionDelivery(userId, username);

        // Verify delivery happens within 2 seconds
        verify(messagingTemplate, timeout(2000))
                .convertAndSendToUser(eq(username), eq("/queue/unread-counts"), any());

        long elapsed = System.currentTimeMillis() - startTime;
        assert elapsed < 2000 : "Delivery took longer than 2 seconds: " + elapsed + "ms";
    }
}
