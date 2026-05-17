package com.socialpulse.app.chat.application.usecase;

import com.socialpulse.app.chat.application.dto.request.CreateConversationRequest;
import com.socialpulse.app.chat.application.dto.response.ConversationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface CreateConversationUseCase {
    ConversationResponse createConversation(CreateConversationRequest request,
                                            CustomUserDetails currentUser);
}
