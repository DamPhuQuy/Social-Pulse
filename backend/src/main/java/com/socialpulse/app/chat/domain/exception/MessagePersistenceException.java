package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when a database persistence operation fails for chat messages or conversations.
 * Examples: database connection failure, constraint violation, unexpected persistence errors.
 *
 * Maps to HTTP 500 / STOMP ERROR frame.
 */
public class MessagePersistenceException extends ChatException {

    public MessagePersistenceException(String message) {
        super(message);
    }

    public MessagePersistenceException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "INTERNAL_ERROR";
    }
}
