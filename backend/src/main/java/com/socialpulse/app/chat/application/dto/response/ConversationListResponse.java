package com.socialpulse.app.chat.application.dto.response;

import java.time.Instant;

import lombok.Builder;

@Builder
public record ConversationListResponse(
        Long id,
        Long otherParticipantId,
        String otherParticipantUsername,
        String lastMessagePreview,
        int unreadCount,
        Instant lastMessageAt) {}
