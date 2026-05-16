package com.socialpulse.app.notification.application.service;

import java.time.LocalDateTime;

import com.socialpulse.app.notification.application.usecase.MarkAllNotificationsReadUseCase;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

public class MarkAllNotificationsReadService implements MarkAllNotificationsReadUseCase {
    private final NotificationRepository notificationRepository;

    public MarkAllNotificationsReadService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public void markAllRead(CustomUserDetails currentUser) {
        notificationRepository.markAllRead(currentUser.getId(), LocalDateTime.now());
    }
}
