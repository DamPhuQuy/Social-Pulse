package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when a referenced conversation does not exist.
 *
 * Maps to HTTP 404 / STOMP ERROR frame.
 */
public class ConversationNotFoundException extends ChatException {

    public ConversationNotFoundException(String message) {
        super(message);
    }

    public ConversationNotFoundException(Long conversationId) {
        super("Conversation not found with id: " + conversationId);
    }

    @Override
    public String getErrorCode() {
        return "NOT_FOUND_ERROR";
    }
}
