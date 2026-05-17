package com.socialpulse.app.chat.adapter.persistence;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Pageable;

import com.socialpulse.app.chat.domain.model.Message;
import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.domain.repository.MessageRepository;
import com.socialpulse.app.chat.infrastructure.persistence.mapper.MessagePersistenceMapper;
import com.socialpulse.app.chat.infrastructure.persistence.repository.JpaMessageRepository;

public class MessageRepositoryAdapter implements MessageRepository {

    private final JpaMessageRepository jpaMessageRepository;
    private final MessagePersistenceMapper messagePersistenceMapper;

    public MessageRepositoryAdapter(
            JpaMessageRepository jpaMessageRepository,
            MessagePersistenceMapper messagePersistenceMapper) {
        this.jpaMessageRepository = jpaMessageRepository;
        this.messagePersistenceMapper = messagePersistenceMapper;
    }

    @Override
    public Message save(Message message) {
        var entity = messagePersistenceMapper.toEntity(message);
        var saved = jpaMessageRepository.save(entity);
        return messagePersistenceMapper.toDomain(saved);
    }

    @Override
    public Optional<Message> findById(Long id) {
        return jpaMessageRepository.findById(id)
                .map(messagePersistenceMapper::toDomain);
    }

    @Override
    public List<Message> findByConversationIdBefore(Long conversationId, Instant cursor, int limit) {
        Pageable pageable = Pageable.ofSize(limit);
        return jpaMessageRepository.findByConversationIdAndTimestampBefore(conversationId, cursor, pageable)
                .stream()
                .map(messagePersistenceMapper::toDomain)
                .toList();
    }

    @Override
    public void updateStatus(Long messageId, MessageStatus status) {
        jpaMessageRepository.updateStatus(messageId, status);
    }

    @Override
    public void markAllAsRead(Long conversationId, Long recipientId) {
        jpaMessageRepository.markAllAsRead(conversationId, recipientId);
    }

    @Override
    public long countUnread(Long conversationId, Long userId) {
        return jpaMessageRepository.countUnread(conversationId, userId);
    }
}
