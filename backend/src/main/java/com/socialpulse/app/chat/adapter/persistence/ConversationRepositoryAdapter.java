package com.socialpulse.app.chat.adapter.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.chat.domain.model.Conversation;
import com.socialpulse.app.chat.domain.repository.ConversationRepository;
import com.socialpulse.app.chat.infrastructure.persistence.mapper.ConversationPersistenceMapper;
import com.socialpulse.app.chat.infrastructure.persistence.repository.JpaConversationRepository;

public class ConversationRepositoryAdapter implements ConversationRepository {

    private final JpaConversationRepository jpaConversationRepository;
    private final ConversationPersistenceMapper conversationPersistenceMapper;

    public ConversationRepositoryAdapter(
            JpaConversationRepository jpaConversationRepository,
            ConversationPersistenceMapper conversationPersistenceMapper) {
        this.jpaConversationRepository = jpaConversationRepository;
        this.conversationPersistenceMapper = conversationPersistenceMapper;
    }

    @Override
    public Conversation save(Conversation conversation) {
        var entity = conversationPersistenceMapper.toEntity(conversation);
        var saved = jpaConversationRepository.save(entity);
        return conversationPersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Conversation> findById(Long id) {
        return jpaConversationRepository.findById(id)
                .map(conversationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Conversation> findByParticipants(Long userId1, Long userId2) {
        // Normalize participant order: smaller ID first (matches DB constraint)
        Long participant1 = Math.min(userId1, userId2);
        Long participant2 = Math.max(userId1, userId2);
        return jpaConversationRepository.findByParticipant1IdAndParticipant2Id(participant1, participant2)
                .map(conversationPersistenceMapper::toDomain);
    }

    @Override
    public List<Conversation> findByUserId(Long userId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);
        return jpaConversationRepository.findByParticipant1IdOrParticipant2Id(userId, pageable)
                .map(conversationPersistenceMapper::toDomain)
                .getContent();
    }

    @Override
    public void updateLastMessageTimestamp(Long conversationId, Instant timestamp) {
        jpaConversationRepository.updateLastMessageTimestamp(conversationId, timestamp);
    }
}
