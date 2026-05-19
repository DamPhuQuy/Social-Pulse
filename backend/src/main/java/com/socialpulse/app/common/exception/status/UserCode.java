package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum UserCode implements AppCode {
    USER_ALREADY_EXISTS(400, "User already exists"),
    USER_NOT_FOUND(404, "User not found"),
    USER_PROFILE_ALREADY_EXISTS(400, "User profile already exists"),
    USER_PROFILE_NOT_FOUND(404, "User profile not found"),
    CANNOT_FOLLOW_YOURSELF(400, "Cannot follow yourself"),
    ALREADY_FOLLOWING(400, "Already following this user"),
    NOT_FOLLOWING(400, "Not following this user"),
    CANNOT_BLOCK_YOURSELF(400, "Cannot block yourself"),
    ALREADY_BLOCKED(400, "Already blocked this user"),
    NOT_BLOCKED(400, "Not blocking this user");

    private final int code;
    private final String message;

    UserCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
