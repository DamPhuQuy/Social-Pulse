package com.socialpulse.app.behavior.application.dto;

import com.socialpulse.app.behavior.domain.enums.EventType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.Map;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TrackBehaviorRequest {
    private Long userId;
    private Long postId;
    private EventType eventType;
    private Integer position;
    private Integer dwellTimeSeconds;
    private Map<String, Object> metadata;
}
