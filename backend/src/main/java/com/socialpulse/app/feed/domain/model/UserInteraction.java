package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteraction {
    private Long id;
    private Long viewerId;
    private Long authorId;
    private String interactionType;
    private LocalDateTime createdAt;
}
