package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum SystemCode implements AppCode {
    INTERNAL_SERVER_ERROR(500, "Internal server error"),
    EMAIL_SENDS_FAILED(500, "Failed to send email"),
    UPLOAD_FAILED(500, "Failed to upload file to Cloudinary");

    private final int code;
    private final String message;

    SystemCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
