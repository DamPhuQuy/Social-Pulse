package com.socialpulse.app.notification.application.dto.response;

import java.time.LocalDateTime;

import com.socialpulse.app.notification.domain.enums.NotificationResourceType;
import com.socialpulse.app.notification.domain.enums.NotificationType;
import com.socialpulse.app.user.application.dto.response.UserSummary;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationResponse {
    private Long id;
    private UserSummary actor;
    private Long recipientId;
    private NotificationType type;
    private NotificationResourceType resourceType;
    private Long resourceId;
    private String message;
    private boolean read;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;
}
