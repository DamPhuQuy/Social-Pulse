package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when a user attempts to access a conversation they are not a participant of.
 * Examples: sending a message to someone else's conversation, reading history of
 * a conversation the user doesn't belong to.
 *
 * Maps to HTTP 403 / STOMP ERROR frame.
 */
public class UnauthorizedChatAccessException extends ChatException {

    public UnauthorizedChatAccessException(String message) {
        super(message);
    }

    public UnauthorizedChatAccessException(Long userId, Long conversationId) {
        super("User " + userId + " is not a participant of conversation " + conversationId);
    }

    @Override
    public String getErrorCode() {
        return "AUTHORIZATION_ERROR";
    }
}
