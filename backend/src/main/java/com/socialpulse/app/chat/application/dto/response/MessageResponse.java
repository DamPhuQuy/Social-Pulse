package com.socialpulse.app.chat.application.dto.response;

import java.time.Instant;

import com.socialpulse.app.chat.domain.model.MessageStatus;

import lombok.Builder;

@Builder
public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        Instant timestamp,
        MessageStatus status) {}
