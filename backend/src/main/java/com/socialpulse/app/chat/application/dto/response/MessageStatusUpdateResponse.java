package com.socialpulse.app.chat.application.dto.response;

import java.time.Instant;

import com.socialpulse.app.chat.domain.model.MessageStatus;

import lombok.Builder;

/**
 * DTO for notifying the sender about a message status change.
 * Sent via WebSocket to the sender when the recipient delivers or reads a message.
 */
@Builder
public record MessageStatusUpdateResponse(
        Long messageId,
        Long conversationId,
        MessageStatus previousStatus,
        MessageStatus newStatus,
        Instant updatedAt) {}
