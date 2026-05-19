package com.socialpulse.app.chat.application.dto.response;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ConversationResponse(
        Long id,
        Long participant1Id,
        Long participant2Id,
        Instant createdAt,
        Instant lastMessageAt) {}
