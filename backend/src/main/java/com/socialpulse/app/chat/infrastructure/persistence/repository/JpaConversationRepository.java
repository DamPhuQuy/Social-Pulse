package com.socialpulse.app.chat.infrastructure.persistence.repository;

import java.time.Instant;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.chat.infrastructure.persistence.entity.ConversationEntity;

@Repository
public interface JpaConversationRepository extends JpaRepository<ConversationEntity, Long> {

    Optional<ConversationEntity> findByParticipant1IdAndParticipant2Id(Long participant1Id, Long participant2Id);

    @Query("""
            SELECT c FROM ConversationEntity c
            WHERE c.participant1Id = :userId OR c.participant2Id = :userId
            ORDER BY c.lastMessageAt DESC NULLS LAST
            """)
    Page<ConversationEntity> findByParticipant1IdOrParticipant2Id(@Param("userId") Long userId, Pageable pageable);

    @Modifying
    @Query("""
            UPDATE ConversationEntity c
            SET c.lastMessageAt = :timestamp
            WHERE c.id = :conversationId
            """)
    void updateLastMessageTimestamp(@Param("conversationId") Long conversationId, @Param("timestamp") Instant timestamp);
}
