package com.socialpulse.app.notification.adapter.persistence;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.notification.infrastructure.persistence.mapper.NotificationPersistenceMapper;
import com.socialpulse.app.notification.infrastructure.persistence.repository.JpaNotificationRepository;

public class NotificationRepositoryAdapter implements NotificationRepository {
    private final JpaNotificationRepository jpaNotificationRepository;
    private final NotificationPersistenceMapper notificationPersistenceMapper;

    public NotificationRepositoryAdapter(
            JpaNotificationRepository jpaNotificationRepository,
            NotificationPersistenceMapper notificationPersistenceMapper) {
        this.jpaNotificationRepository = jpaNotificationRepository;
        this.notificationPersistenceMapper = notificationPersistenceMapper;
    }

    @Override
    public Notification save(Notification notification) {
        return notificationPersistenceMapper.toDomain(
                jpaNotificationRepository.save(notificationPersistenceMapper.toEntity(notification)));
    }

    @Override
    public Page<Notification> findByRecipientId(Long recipientId, Pageable pageable) {
        return jpaNotificationRepository.findByRecipientIdOrderByCreatedAtDesc(recipientId, pageable)
                .map(notificationPersistenceMapper::toDomain);
    }

    @Override
    public Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId) {
        return jpaNotificationRepository.findByIdAndRecipientId(id, recipientId)
                .map(notificationPersistenceMapper::toDomain);
    }

    @Override
    public long countUnreadByRecipientId(Long recipientId) {
        return jpaNotificationRepository.countByRecipientIdAndReadAtIsNull(recipientId);
    }

    @Override
    public void markAllRead(Long recipientId, LocalDateTime readAt) {
        jpaNotificationRepository.markAllRead(recipientId, readAt);
    }
}
