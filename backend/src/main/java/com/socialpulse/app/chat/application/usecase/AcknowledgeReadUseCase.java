package com.socialpulse.app.chat.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface AcknowledgeReadUseCase {
    void acknowledgeRead(Long conversationId, CustomUserDetails currentUser);
}
