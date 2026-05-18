package com.socialpulse.app.common.websocket;

public class MaxWebSocketSessionsException extends RuntimeException {
    public MaxWebSocketSessionsException(Long userId) {
        super("User " + userId + " has reached the maximum number of WebSocket sessions");
    }
}
