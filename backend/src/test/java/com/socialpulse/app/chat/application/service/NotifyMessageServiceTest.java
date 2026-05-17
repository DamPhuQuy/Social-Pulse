package com.socialpulse.app.chat.application.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import com.socialpulse.app.chat.application.dto.response.MessageResponse;
import com.socialpulse.app.chat.domain.event.MessagePersistedEvent;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.chat.infrastructure.websocket.WebSocketSessionManager;

@ExtendWith(MockitoExtension.class)
class NotifyMessageServiceTest {

    @Mock
    private WebSocketSessionManager sessionManager;

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Mock
    private MessageRepository messageRepository;

    @Mock
    private StringRedisTemplate redisTemplate;

    @Mock
    private ValueOperations<String, String> valueOperations;

    private NotifyMessageService notifyMessageService;

    @BeforeEach
    void setUp() {
        notifyMessageService = new NotifyMessageService(
                sessionManager, messagingTemplate, messageRepository, redisTemplate);
    }

    @Test
    void onMessagePersisted_recipientOnline_deliversViaStompAndUpdatesStatus() {
        Message message = Message.builder()
                .id(1L)
                .conversationId(10L)
                .senderId(100L)
                .content("Hello!")
                .timestamp(Instant.now())
                .status(MessageStatus.SENT)
                .build();
        Long recipientId = 200L;
        MessagePersistedEvent event = new MessagePersistedEvent(message, recipientId);

        when(sessionManager.isUserOnline(recipientId)).thenReturn(true);

        notifyMessageService.onMessagePersisted(event);

        verify(messagingTemplate).convertAndSend(eq("/topic/chat.10"), any(MessageResponse.class));
        verify(messageRepository).updateStatus(1L, MessageStatus.DELIVERED);
        verify(redisTemplate, never()).opsForValue();
    }

    @Test
    void onMessagePersisted_recipientOffline_incrementsUnreadCount() {
        Message message = Message.builder()
                .id(2L)
                .conversationId(20L)
                .senderId(100L)
                .content("Are you there?")
                .timestamp(Instant.now())
                .status(MessageStatus.SENT)
                .build();
        Long recipientId = 300L;
        MessagePersistedEvent event = new MessagePersistedEvent(message, recipientId);

        when(sessionManager.isUserOnline(recipientId)).thenReturn(false);
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        notifyMessageService.onMessagePersisted(event);

        verify(messagingTemplate, never()).convertAndSend(any(String.class), any(MessageResponse.class));
        verify(messageRepository, never()).updateStatus(any(), any());
        verify(valueOperations).increment("chat:unread:20:300");
    }

    @Test
    void onMessagePersisted_deliveryFails_incrementsUnreadCount() {
        Message message = Message.builder()
                .id(3L)
                .conversationId(30L)
                .senderId(100L)
                .content("This will fail")
                .timestamp(Instant.now())
                .status(MessageStatus.SENT)
                .build();
        Long recipientId = 400L;
        MessagePersistedEvent event = new MessagePersistedEvent(message, recipientId);

        when(sessionManager.isUserOnline(recipientId)).thenReturn(true);
        doThrow(new RuntimeException("WebSocket delivery failed"))
                .when(messagingTemplate).convertAndSend(eq("/topic/chat.30"), any(MessageResponse.class));
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        notifyMessageService.onMessagePersisted(event);

        verify(messageRepository, never()).updateStatus(any(), any());
        verify(valueOperations).increment("chat:unread:30:400");
    }
}
