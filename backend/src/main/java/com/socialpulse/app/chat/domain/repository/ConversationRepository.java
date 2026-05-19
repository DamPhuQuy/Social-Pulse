package com.socialpulse.app.chat.domain.repository;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import com.socialpulse.app.chat.domain.model.Conversation;

public interface ConversationRepository {
    Conversation save(Conversation conversation);
    Optional<Conversation> findById(Long id);
    Optional<Conversation> findByParticipants(Long userId1, Long userId2);
    List<Conversation> findByUserId(Long userId, int page, int size);
    void updateLastMessageTimestamp(Long conversationId, Instant timestamp);
}
