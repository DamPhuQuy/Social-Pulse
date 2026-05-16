package com.socialpulse.app.notification.domain.model;

import java.time.LocalDateTime;

import com.socialpulse.app.notification.domain.enums.NotificationResourceType;
import com.socialpulse.app.notification.domain.enums.NotificationType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {
    private Long id;
    private Long recipientId;
    private Long actorId;
    private NotificationType type;
    private NotificationResourceType resourceType;
    private Long resourceId;
    private String message;
    private LocalDateTime createdAt;
    private LocalDateTime readAt;

    public void markRead() {
        if (this.readAt == null) {
            this.readAt = LocalDateTime.now();
        }
    }

    public boolean isRead() {
        return this.readAt != null;
    }
}
