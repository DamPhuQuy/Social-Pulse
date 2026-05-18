package com.socialpulse.app.common.exception.status;

import lombok.Getter;

@Getter
public enum DiscoveryCode implements AppCode {
    SEARCH_HISTORY_NOT_FOUND(404, "Search history not found");

    private final int code;
    private final String message;

    DiscoveryCode(int code, String message) {
        this.code = code;
        this.message = message;
    }
}
