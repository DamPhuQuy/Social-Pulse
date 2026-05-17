package com.socialpulse.app.chat.application.usecase;

import java.util.List;

import com.socialpulse.app.chat.application.dto.response.ConversationListResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetConversationsUseCase {
    List<ConversationListResponse> getConversations(int page, int size, CustomUserDetails currentUser);
}
