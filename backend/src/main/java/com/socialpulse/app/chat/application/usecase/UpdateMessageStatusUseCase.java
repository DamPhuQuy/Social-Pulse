package com.socialpulse.app.chat.application.usecase;

import com.socialpulse.app.chat.domain.model.MessageStatus;
import com.socialpulse.app.security.user.CustomUserDetails;

/**
 * Use case for updating the delivery/read status of a message.
 * Validates that the status transition follows the valid order: SENT → DELIVERED → READ.
 */
public interface UpdateMessageStatusUseCase {
    void updateStatus(Long messageId, MessageStatus newStatus, CustomUserDetails currentUser);
}
