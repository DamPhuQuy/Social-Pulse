package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum ReportCode implements AppCode {
    REPORT_NOT_FOUND(404, "Report not found"),
    REPORT_TARGET_NOT_FOUND(404, "Report target not found"),
    REPORT_STATUS_UPDATE_NOT_ALLOWED(400, "Report status update is not allowed");

    private final int code;
    private final String message;

    ReportCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
