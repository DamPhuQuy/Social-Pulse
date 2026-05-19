package com.socialpulse.app.chat.domain.model;

import java.time.Instant;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Conversation {
    private Long id;
    private Long participant1Id;
    private Long participant2Id;
    private Instant createdAt;
    private Instant lastMessageAt;

    public boolean hasParticipant(Long userId) {
        return participant1Id.equals(userId) || participant2Id.equals(userId);
    }

    public Long getOtherParticipant(Long userId) {
        return participant1Id.equals(userId) ? participant2Id : participant1Id;
    }

    public void updateLastMessageTimestamp(Instant timestamp) {
        this.lastMessageAt = timestamp;
    }
}
