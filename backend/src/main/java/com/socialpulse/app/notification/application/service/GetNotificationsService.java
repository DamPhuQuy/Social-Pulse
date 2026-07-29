package com.socialpulse.app.notification.application.service;
import org.springframework.stereotype.Service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.notification.application.dto.response.NotificationResponse;
import com.socialpulse.app.notification.application.usecase.GetNotificationsUseCase;
import com.socialpulse.app.notification.domain.model.Notification;
import com.socialpulse.app.notification.domain.repository.NotificationRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetNotificationsService implements GetNotificationsUseCase {
    private final NotificationRepository notificationRepository;
    private final NotificationResponseAssembler notificationResponseAssembler;

    public GetNotificationsService(
            NotificationRepository notificationRepository,
            NotificationResponseAssembler notificationResponseAssembler) {
        this.notificationRepository = notificationRepository;
        this.notificationResponseAssembler = notificationResponseAssembler;
    }

    @Override
    public PageResponse<NotificationResponse> getNotifications(int page, int size, CustomUserDetails currentUser) {
        Page<Notification> notifications = notificationRepository.findByRecipientId(
                currentUser.getId(),
                PageRequest.of(page, size));

        return PageResponse.<NotificationResponse>builder()
                .items(notificationResponseAssembler.assemble(notifications.getContent()))
                .page(notifications.getNumber())
                .size(notifications.getSize())
                .totalElements(notifications.getTotalElements())
                .totalPages(notifications.getTotalPages())
                .hasNext(notifications.hasNext())
                .build();
    }
}
