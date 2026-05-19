package com.socialpulse.app.chat.application.dto.response;

import java.util.List;

import lombok.Builder;

@Builder
public record MessageHistoryResponse(
        List<MessageResponse> messages,
        String nextCursor,
        boolean hasMore) {}
