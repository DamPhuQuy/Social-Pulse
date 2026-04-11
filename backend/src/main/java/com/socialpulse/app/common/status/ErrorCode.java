package com.socialpulse.app.common.status;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_ALREADY_EXISTS(400, "User already exists"),
    USER_NOT_FOUND(404, "User not found"),

    PASSWORD_NOT_MATCH(400, "Passwords do not match"),

    INVALID_CREDENTIALS(404, "Invalid username or password"),
    ACCOUNT_LOCKED(423, "Account is locked"),
    // Tài khoản chưa xác thực email → 403 Forbidden
    ACCOUNT_NOT_VERIFIED(403, "Account is not verified. Please verify your email first."),
    // JWT không hợp lệ hoặc hết hạn → 401 Unauthorized
    INVALID_TOKEN(401, "Invalid or expired token"),
    // Refresh Token không hợp lệ, hết hạn, hoặc không đúng loại → 401 Unauthorized
    INVALID_REFRESH_TOKEN(401, "Invalid or expired refresh token"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),

    OTP_EXPIRED(400, "OTP has expired"),
    OTP_INVALID(400, "Invalid OTP"),
    OTP_TOO_MANY_ATTEMPTS(429, "Too many OTP attempts"),

    EMAIL_SENDS_FAILED(500, "Failed to send email"),

    UPLOAD_FAILED(500, "Failed to upload file to Cloudinary");

    private final int code;
    private final String message;

    ErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

    public int getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
