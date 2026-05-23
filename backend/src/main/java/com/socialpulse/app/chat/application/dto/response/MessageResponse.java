package com.socialpulse.app.chat.application.dto.response;

import java.time.Instant;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.socialpulse.app.chat.domain.model.MessageStatus;

import lombok.Builder;

@Builder
public record MessageResponse(
        Long id,
        Long conversationId,
        Long senderId,
        String content,
        @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSS'Z'", timezone = "UTC")
        Instant timestamp,
        MessageStatus status) {}
