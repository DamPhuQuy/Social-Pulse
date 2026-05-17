package com.socialpulse.app.chat.application.service;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import com.socialpulse.app.chat.application.dto.response.MessageResponse;
import com.socialpulse.app.chat.domain.event.MessagePersistedEvent;
import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.chat.infrastructure.websocket.WebSocketSessionManager;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles real-time message delivery after a message is persisted.
 *
 * <p>Listens for {@link MessagePersistedEvent} domain events and:
 * <ul>
 *   <li>If recipient is online: delivers via STOMP and updates status to DELIVERED</li>
 *   <li>If recipient is offline (or delivery fails): increments unread count in Redis</li>
 * </ul>
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class NotifyMessageService {

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final String TOPIC_PREFIX = "/topic/chat.";

    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final MessageRepository messageRepository;
    private final StringRedisTemplate redisTemplate;

    /**
     * Processes a message-persisted event after the originating transaction commits.
     * Attempts real-time delivery to the recipient if they are online.
     *
     * @param event the message-persisted domain event
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onMessagePersisted(MessagePersistedEvent event) {
        Message message = event.message();
        Long recipientId = event.recipientId();
        Long conversationId = message.getConversationId();

        if (sessionManager.isUserOnline(recipientId)) {
            try {
                MessageResponse payload = buildMessageResponse(message);
                String destination = TOPIC_PREFIX + conversationId;
                messagingTemplate.convertAndSend(destination, payload);

                messageRepository.updateStatus(message.getId(), MessageStatus.DELIVERED);
                log.debug("Message {} delivered to recipient {} on conversation {}",
                        message.getId(), recipientId, conversationId);
            } catch (Exception e) {
                log.warn("Failed to deliver message {} to recipient {}: {}",
                        message.getId(), recipientId, e.getMessage());
                incrementUnreadCount(conversationId, recipientId);
            }
        } else {
            incrementUnreadCount(conversationId, recipientId);
        }
    }

    private void incrementUnreadCount(Long conversationId, Long recipientId) {
        String key = UNREAD_KEY_PREFIX + conversationId + ":" + recipientId;
        redisTemplate.opsForValue().increment(key);
        log.debug("Incremented unread count for conversation {} recipient {}",
                conversationId, recipientId);
    }

    private MessageResponse buildMessageResponse(Message message) {
        return MessageResponse.builder()
                .id(message.getId())
                .conversationId(message.getConversationId())
                .senderId(message.getSenderId())
                .content(message.getContent())
                .timestamp(message.getTimestamp())
                .status(message.getStatus())
                .build();
    }
}
