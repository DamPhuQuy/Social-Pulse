package com.socialpulse.app.notification.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.notification.infrastructure.persistence.entity.NotificationEntity;

@Repository
public interface JpaNotificationRepository extends JpaRepository<NotificationEntity, Long> {
    Page<NotificationEntity> findByRecipientIdOrderByCreatedAtDesc(Long recipientId, Pageable pageable);

    Optional<NotificationEntity> findByIdAndRecipientId(Long id, Long recipientId);

    long countByRecipientIdAndReadAtIsNull(Long recipientId);

    @Modifying
    @Query("""
            UPDATE NotificationEntity n
            SET n.readAt = :readAt
            WHERE n.recipientId = :recipientId
              AND n.readAt IS NULL
            """)
    void markAllRead(@Param("recipientId") Long recipientId, @Param("readAt") LocalDateTime readAt);
}
