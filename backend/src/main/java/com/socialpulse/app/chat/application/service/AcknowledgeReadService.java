package com.socialpulse.app.chat.application.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.chat.application.usecase.AcknowledgeReadUseCase;
import com.socialpulse.app.chat.domain.exception.ConversationNotFoundException;
import com.socialpulse.app.chat.domain.exception.UnauthorizedChatAccessException;
import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.common.websocket.WebSocketSessionManager;
import com.socialpulse.app.security.user.CustomUserDetails;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Handles read acknowledgment for a conversation.
 *
 * <p>When a recipient acknowledges reading a conversation:
 * <ul>
 *   <li>All unread messages from the other participant are marked as READ</li>
 *   <li>The unread count in Redis is reset</li>
 *   <li>The sender is notified of the bulk status change via WebSocket (if online)</li>
 * </ul>
 */
@Slf4j
@Service
@Transactional
@RequiredArgsConstructor
public class AcknowledgeReadService implements AcknowledgeReadUseCase {

    private static final String UNREAD_KEY_PREFIX = "chat:unread:";
    private static final String TOPIC_PREFIX = "/topic/chat.";

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final WebSocketSessionManager sessionManager;
    private final SimpMessagingTemplate messagingTemplate;
    private final StringRedisTemplate redisTemplate;

    @Override
    public void acknowledgeRead(Long conversationId, CustomUserDetails currentUser) {
        Long currentUserId = currentUser.getId();

        // 1. Find conversation by ID (throw ConversationNotFoundException if not found)
        Conversation conversation = conversationRepository.findById(conversationId)
                .orElseThrow(() -> new ConversationNotFoundException(conversationId));

        // 2. Verify current user is a participant
        if (!conversation.hasParticipant(currentUserId)) {
            throw new UnauthorizedChatAccessException(currentUserId, conversationId);
        }

        // 3. Mark all unread messages as READ
        //    This marks all messages where senderId != currentUserId and status != READ
        messageRepository.markAllAsRead(conversationId, currentUserId);

        // 4. Reset unread count in Redis
        String unreadKey = UNREAD_KEY_PREFIX + conversationId + ":" + currentUserId;
        redisTemplate.delete(unreadKey);

        // 5. Notify the other participant (sender) of the bulk status change
        Long otherParticipantId = conversation.getOtherParticipant(currentUserId);
        if (sessionManager.isUserOnline(otherParticipantId)) {
            try {
                String destination = TOPIC_PREFIX + conversationId;
                Object payload = Map.of(
                        "type", "READ_ACK",
                        "conversationId", conversationId,
                        "readBy", currentUserId,
                        "timestamp", Instant.now().toString()
                );
                messagingTemplate.convertAndSend(destination, payload);
                log.debug("Notified user {} of read acknowledgment in conversation {}",
                        otherParticipantId, conversationId);
            } catch (Exception e) {
                log.warn("Failed to notify user {} of read acknowledgment in conversation {}: {}",
                        otherParticipantId, conversationId, e.getMessage());
            }
        }
    }
}
