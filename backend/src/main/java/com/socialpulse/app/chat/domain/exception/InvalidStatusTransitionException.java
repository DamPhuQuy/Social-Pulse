package com.socialpulse.app.chat.domain.exception;

/**
 * Thrown when an invalid message status transition is attempted.
 * Valid transitions follow the order: SENT → DELIVERED → READ.
 * Backward transitions (e.g., READ → SENT) or invalid skips are rejected.
 *
 * Maps to HTTP 409 / STOMP ERROR frame.
 */
public class InvalidStatusTransitionException extends ChatException {

    public InvalidStatusTransitionException(String message) {
        super(message);
    }

    public InvalidStatusTransitionException(String currentStatus, String targetStatus) {
        super("Invalid status transition from " + currentStatus + " to " + targetStatus);
    }

    @Override
    public String getErrorCode() {
        return "CONFLICT_ERROR";
    }
}
