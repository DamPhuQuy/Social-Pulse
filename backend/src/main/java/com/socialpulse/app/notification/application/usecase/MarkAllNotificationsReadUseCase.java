package com.socialpulse.app.notification.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface MarkAllNotificationsReadUseCase {
    void markAllRead(CustomUserDetails currentUser);
}
