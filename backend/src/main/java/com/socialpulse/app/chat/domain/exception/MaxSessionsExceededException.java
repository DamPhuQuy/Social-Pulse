package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when a user attempts to establish a new WebSocket session
 * but has already reached the maximum allowed concurrent sessions (5).
 *
 * Maps to HTTP 409 / STOMP ERROR frame (connection rejected).
 */
public class MaxSessionsExceededException extends ChatException {

    private static final int MAX_SESSIONS = 5;

    public MaxSessionsExceededException(Long userId) {
        super(String.format("User %d has reached the maximum of %d concurrent sessions", userId, MAX_SESSIONS));
    }

    public MaxSessionsExceededException(Long userId, Throwable cause) {
        super(String.format("User %d has reached the maximum of %d concurrent sessions", userId, MAX_SESSIONS), cause);
    }

    @Override
    public String getErrorCode() {
        return "MAX_SESSIONS_EXCEEDED";
    }
}
