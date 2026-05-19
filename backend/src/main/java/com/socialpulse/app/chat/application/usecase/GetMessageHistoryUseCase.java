package com.socialpulse.app.chat.application.usecase;

import com.socialpulse.app.chat.application.dto.response.MessageHistoryResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetMessageHistoryUseCase {
    MessageHistoryResponse getHistory(Long conversationId, String cursor,
                                      int size, CustomUserDetails currentUser);
}
