package com.socialpulse.app.behavior.domain.model;

import com.socialpulse.app.behavior.domain.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehavior {
    private Long id;
    private Long userId;
    private Long postId;
    private EventType eventType;
    private LocalDateTime eventTime;
    private Integer position;
    private Integer dwellTimeSeconds;
    private Map<String, Object> metadata;
}
