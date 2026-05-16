package com.socialpulse.app.notification.application.usecase;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetNotificationsUseCase {
    PageResponse<NotificationResponse> getNotifications(int page, int size, CustomUserDetails currentUser);
}
