package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserInteractionAggregate {
    private Long authorId;
    private long interactionCount7d;
    private long interactionCount30d;
    private LocalDateTime latestInteractionTime;
}
