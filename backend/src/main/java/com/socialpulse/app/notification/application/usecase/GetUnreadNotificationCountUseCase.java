package com.socialpulse.app.notification.application.usecase;

import com.socialpulse.app.notification.application.dto.response.NotificationUnreadCountResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface GetUnreadNotificationCountUseCase {
    NotificationUnreadCountResponse getUnreadCount(CustomUserDetails currentUser);
}
