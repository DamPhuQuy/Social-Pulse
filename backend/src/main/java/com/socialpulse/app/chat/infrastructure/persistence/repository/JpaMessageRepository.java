package com.socialpulse.app.chat.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.chat.infrastructure.persistence.entity.MessageEntity;

@Repository
public interface JpaMessageRepository extends JpaRepository<MessageEntity, Long> {

    @Query("""
            SELECT m FROM MessageEntity m
            WHERE m.conversationId = :conversationId AND m.timestamp < :cursor
            ORDER BY m.timestamp DESC
            """)
    List<MessageEntity> findByConversationIdAndTimestampBefore(
            @Param("conversationId") Long conversationId,
            @Param("cursor") Instant cursor,
            Pageable pageable);

    @Modifying
    @Query("""
            UPDATE MessageEntity m
            SET m.status = :status
            WHERE m.id = :messageId
            """)
    void updateStatus(@Param("messageId") Long messageId, @Param("status") MessageStatus status);

    @Modifying
    @Query("""
            UPDATE MessageEntity m
            SET m.status = 'READ'
            WHERE m.conversationId = :conversationId
              AND m.senderId != :recipientId
              AND m.status != 'READ'
            """)
    void markAllAsRead(@Param("conversationId") Long conversationId, @Param("recipientId") Long recipientId);

    @Query("""
            SELECT COUNT(m) FROM MessageEntity m
            WHERE m.conversationId = :conversationId
              AND m.senderId != :userId
              AND m.status != 'READ'
            """)
    long countUnread(@Param("conversationId") Long conversationId, @Param("userId") Long userId);
}
