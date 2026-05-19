package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum PostCode implements AppCode {
    POST_NOT_FOUND(404, "Post not found"),

    POST_NOT_ACCESSIBLE(403, "Post is not accessible"),

    POST_ALREADY_SHARED(409, "Post is already shared by current user"),

    POST_TOPIC_INVALID(400, "Post topics are invalid");

    private final int code;
    private final String message;

    PostCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
