package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum NotificationCode implements AppCode {
    NOTIFICATION_NOT_FOUND(404, "Notification not found");

    private final int code;
    private final String message;

    NotificationCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
