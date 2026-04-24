package com.socialpulse.app.behavior.domain.model;

import com.socialpulse.app.behavior.domain.enums.EventType;

import java.time.LocalDateTime;
import java.util.Map;

public class UserBehavior {
    private Long userId;
    private Long postId;
    private EventType eventType;

    private LocalDateTime eventTime;
    private Integer position;
    private Map<String, Object> metadata;
}
