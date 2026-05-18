package com.socialpulse.app.chat.application.dto.request;

import jakarta.validation.constraints.NotNull;

public record CreateConversationRequest(
        @NotNull(message = "Participant ID must not be null") Long participantId) {}
