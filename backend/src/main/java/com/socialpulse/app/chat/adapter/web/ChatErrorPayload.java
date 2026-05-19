package com.socialpulse.app.chat.adapter.web;

import java.time.Instant;

/**
 * Error payload sent to clients via WebSocket when a ChatException occurs.
 * Delivered to the user's personal error queue: /user/queue/errors
 */
public record ChatErrorPayload(
        String errorCode,
        String message,
        Instant timestamp) {

    public static ChatErrorPayload of(String errorCode, String message) {
        return new ChatErrorPayload(errorCode, message, Instant.now());
    }
}
