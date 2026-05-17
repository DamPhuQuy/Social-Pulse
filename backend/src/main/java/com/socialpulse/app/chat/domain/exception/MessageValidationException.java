package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when message content fails validation rules.
 * Examples: empty/whitespace-only content, content exceeding 2000 characters,
 * invalid cursor format, self-conversation attempt.
 *
 * Maps to HTTP 400 / STOMP ERROR frame.
 */
public class MessageValidationException extends ChatException {

    public MessageValidationException(String message) {
        super(message);
    }

    public MessageValidationException(String message, Throwable cause) {
        super(message, cause);
    }

    @Override
    public String getErrorCode() {
        return "VALIDATION_ERROR";
    }
}
