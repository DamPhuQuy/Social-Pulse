package com.socialpulse.app.behavior.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserInteractionFeatures {
    private Long userId;
    private Long authorId;

    // Relationship features
    private boolean follows;
    private int interactionCount7d;
    private int interactionCount30d;
    private double hoursSinceLastInteraction;
    private double affinityScore;
}
