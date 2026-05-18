package com.socialpulse.app.chat.application.service;

import java.time.Instant;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.chat.application.dto.response.MessageStatusUpdateResponse;
import com.socialpulse.app.chat.application.usecase.UpdateMessageStatusUseCase;
import com.socialpulse.app.chat.domain.exception.InvalidStatusTransitionException;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.common.websocket.WebSocketSessionManager;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;

/**
 * Service responsible for updating message delivery/read status.
 * Validates transitions follow the valid order (SENT → DELIVERED → READ),
 * persists the update, and notifies the sender via WebSocket or queues
 * the notification for delivery on reconnection.
 */
@Service
@Transactional
@RequiredArgsConstructor
public class UpdateMessageStatusService implements UpdateMessageStatusUseCase {

    private static final String PENDING_STATUS_KEY_PREFIX = "chat:pending-status:";

    private final MessageRepository messageRepository;
    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void updateStatus(Long messageId, MessageStatus newStatus, CustomUserDetails currentUser) {
        // 1. Find message by ID
        Message message = messageRepository.findById(messageId)
                .orElseThrow(() -> new InvalidStatusTransitionException(
                        "Message not found with id: " + messageId));

        // 2. Validate transition
        MessageStatus previousStatus = message.getStatus();
        if (!message.canTransitionTo(newStatus)) {
            throw new InvalidStatusTransitionException(
                    previousStatus.name(), newStatus.name());
        }

        // 3. Persist the status update
        messageRepository.updateStatus(messageId, newStatus);

        // 4. Notify the sender of the status change
        Long senderId = message.getSenderId();
        MessageStatusUpdateResponse statusUpdate = MessageStatusUpdateResponse.builder()
                .messageId(messageId)
                .conversationId(message.getConversationId())
                .previousStatus(previousStatus)
                .newStatus(newStatus)
                .updatedAt(Instant.now())
                .build();

        if (sessionManager.isUserOnline(senderId)) {
            // Sender is online: send status update via WebSocket
            messagingTemplate.convertAndSend(
                    "/topic/chat." + message.getConversationId(),
                    statusUpdate);
        } else {
            // Sender is offline: queue status update in Redis for delivery on reconnection
            String pendingKey = PENDING_STATUS_KEY_PREFIX + senderId;
            String payload = serializeStatusUpdate(statusUpdate);
            redisTemplate.opsForList().rightPush(pendingKey, payload);
        }
    }

    /**
     * Serializes a status update to a simple JSON-like string for Redis storage.
     * Uses a straightforward format that can be deserialized on reconnection.
     */
    private String serializeStatusUpdate(MessageStatusUpdateResponse update) {
        return String.format(
                "{\"messageId\":%d,\"conversationId\":%d,\"previousStatus\":\"%s\",\"newStatus\":\"%s\",\"updatedAt\":\"%s\"}",
                update.messageId(),
                update.conversationId(),
                update.previousStatus().name(),
                update.newStatus().name(),
                update.updatedAt().toString());
    }
}
