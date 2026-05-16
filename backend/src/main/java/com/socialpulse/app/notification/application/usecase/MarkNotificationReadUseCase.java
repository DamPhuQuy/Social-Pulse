package com.socialpulse.app.notification.application.usecase;

import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.security.user.CustomUserDetails;

public interface MarkNotificationReadUseCase {
    NotificationResponse markRead(Long notificationId, CustomUserDetails currentUser);
}
