package com.socialpulse.app.notification.domain.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.notification.domain.model.Notification;

public interface NotificationRepository {
    Notification save(Notification notification);

    Page<Notification> findByRecipientId(Long recipientId, Pageable pageable);

    Optional<Notification> findByIdAndRecipientId(Long id, Long recipientId);

    long countUnreadByRecipientId(Long recipientId);

    void markAllRead(Long recipientId, LocalDateTime readAt);
}
