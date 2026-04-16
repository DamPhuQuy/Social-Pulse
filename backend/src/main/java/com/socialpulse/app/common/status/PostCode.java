package com.socialpulse.app.common.status;

import lombok.Getter;

@Getter
public enum PostCode implements AppCode {
    POST_NOT_FOUND(404, "Post not found");

    private final int code;
    private final String message;

    PostCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
