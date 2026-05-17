package com.socialpulse.app.chat.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;

public interface MessageRepository {
    Message save(Message message);
    Optional<Message> findById(Long id);
    List<Message> findByConversationIdBefore(Long conversationId, Instant cursor, int limit);
    void updateStatus(Long messageId, MessageStatus status);
    void markAllAsRead(Long conversationId, Long recipientId);
    long countUnread(Long conversationId, Long userId);
}
