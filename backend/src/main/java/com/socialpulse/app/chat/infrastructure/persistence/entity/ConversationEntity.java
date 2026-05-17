package com.socialpulse.app.chat.infrastructure.persistence.entity;

import java.time.Instant;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "conversations", indexes = {
        @Index(name = "idx_conv_participant1", columnList = "participant1_id"),
        @Index(name = "idx_conv_participant2", columnList = "participant2_id"),
        @Index(name = "idx_conv_last_message", columnList = "last_message_at DESC")
}, uniqueConstraints = {
        @UniqueConstraint(name = "uk_conversation_participants",
                columnNames = {"participant1_id", "participant2_id"})
})
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ConversationEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "participant1_id", nullable = false)
    private Long participant1Id;

    @Column(name = "participant2_id", nullable = false)
    private Long participant2Id;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "last_message_at")
    private Instant lastMessageAt;
}
