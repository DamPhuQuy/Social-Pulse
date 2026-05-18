package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum AuthCode implements AppCode {
    PASSWORD_NOT_MATCH(400, "Passwords do not match"),
    INCORRECT_CURRENT_PASSWORD(400, "Current password is incorrect"),
    NEW_PASSWORD_SAME_AS_OLD(400, "New password must be different from current password"),

    INVALID_CREDENTIALS(404, "Invalid username or password"),
    ACCOUNT_LOCKED(423, "Account is locked"),
    ACCOUNT_NOT_VERIFIED(403, "Account is not verified. Please verify your email first."),

    INVALID_TOKEN(401, "Invalid or expired token"),
    INVALID_REFRESH_TOKEN(401, "Invalid or expired refresh token"),
    REFRESH_TOKEN_REUSE_DETECTED(401, "Refresh token reuse detected. Please login again."),

    OTP_EXPIRED(400, "OTP has expired"),
    OTP_INVALID(400, "Invalid OTP"),
    OTP_TOO_MANY_ATTEMPTS(429, "Too many OTP attempts");

    private final int code;
    private final String message;

    AuthCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
