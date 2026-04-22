package com.socialpulse.app.common.exception;

import com.socialpulse.app.common.exception.status.AppCode;

public class AppException extends RuntimeException {
    private final AppCode errorCode;

    public AppException(AppCode errorCode) {
        super(errorCode.getMessage());
        this.errorCode = errorCode;
    }

    public AppCode getErrorCode() {
        return errorCode;
    }
}
