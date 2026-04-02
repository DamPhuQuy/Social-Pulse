package com.socialpulse.app.common.status;

import lombok.Getter;

@Getter
public enum ErrorCode {
    USER_ALREADY_EXISTS(400, "User already exists"),
    USER_NOT_FOUND(404, "User not found"),

    PASSWORD_NOT_MATCH(400, "Passwords do not match"),

    INVALID_CREDENTIALS(404, "Invalid username or password"),
    ACCOUNT_LOCKED(423, "Account is locked"),
    INTERNAL_SERVER_ERROR(500, "Internal server error"),

    OTP_EXPIRED(400, "OTP has expired"),
    OTP_INVALID(400, "Invalid OTP"),
    OTP_TOO_MANY_ATTEMPTS(429, "Too many OTP attempts");

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
