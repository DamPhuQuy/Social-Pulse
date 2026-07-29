package com.socialpulse.app.notification.application.service;
import org.springframework.stereotype.Service;

import java.util.List;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.NotificationCode;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.notification.application.usecase.MarkNotificationReadUseCase;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class MarkNotificationReadService implements MarkNotificationReadUseCase {
    private final NotificationRepository notificationRepository;
    private final NotificationResponseAssembler notificationResponseAssembler;

    public MarkNotificationReadService(
            NotificationRepository notificationRepository,
            NotificationResponseAssembler notificationResponseAssembler) {
        this.notificationRepository = notificationRepository;
        this.notificationResponseAssembler = notificationResponseAssembler;
    }

    @Override
    public NotificationResponse markRead(Long notificationId, CustomUserDetails currentUser) {
        Notification notification = notificationRepository.findByIdAndRecipientId(notificationId, currentUser.getId())
                .orElseThrow(() -> new AppException(NotificationCode.NOTIFICATION_NOT_FOUND));

        notification.markRead();
        Notification savedNotification = notificationRepository.save(notification);

        return notificationResponseAssembler.assemble(List.of(savedNotification)).get(0);
    }
}
