package com.socialpulse.app.chat.application.usecase;

import com.socialpulse.app.chat.application.dto.request.SendMessageRequest;
import com.socialpulse.app.chat.application.dto.response.MessageResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface SendMessageUseCase {
    MessageResponse sendMessage(Long conversationId, SendMessageRequest request,
                                CustomUserDetails sender);
}
