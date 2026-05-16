package com.socialpulse.app.notification.application.service;

import com.socialpulse.app.notification.application.dto.response.NotificationUnreadCountResponse;
import com.socialpulse.app.notification.application.usecase.GetUnreadNotificationCountUseCase;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

public class GetUnreadNotificationCountService implements GetUnreadNotificationCountUseCase {
    private final NotificationRepository notificationRepository;

    public GetUnreadNotificationCountService(NotificationRepository notificationRepository) {
        this.notificationRepository = notificationRepository;
    }

    @Override
    public NotificationUnreadCountResponse getUnreadCount(CustomUserDetails currentUser) {
        return NotificationUnreadCountResponse.builder()
                .unreadCount(notificationRepository.countUnreadByRecipientId(currentUser.getId()))
                .build();
    }
}
