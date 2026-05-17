package com.socialpulse.app.chat.domain.exception;

/**
 * Abstract base exception for all chat-related errors.
 * Provides a structured error code for both HTTP and WebSocket (STOMP) error handling.
 */
public abstract class ChatException extends RuntimeException {

    protected ChatException(String message) {
        super(message);
    }

    protected ChatException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * Returns the error code category for this exception.
     * Used to determine HTTP status codes and STOMP error frame content.
     *
     * @return the error code string (e.g., "VALIDATION_ERROR", "NOT_FOUND_ERROR")
     */
    public abstract String getErrorCode();
}
