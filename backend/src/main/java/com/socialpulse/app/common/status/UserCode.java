package com.socialpulse.app.common.status;

import lombok.Getter;

@Getter
public enum UserCode implements AppCode {
    USER_ALREADY_EXISTS(400, "User already exists"),
    USER_NOT_FOUND(404, "User not found"),
    USER_PROFILE_NOT_FOUND(404, "User profile not found");

    private final int code;
    private final String message;

    UserCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
