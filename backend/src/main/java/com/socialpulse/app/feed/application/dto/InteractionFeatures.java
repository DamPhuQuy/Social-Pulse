package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InteractionFeatures {
    private Long viewerId;
    private Long authorId;

    // Relationship (from UserBehavior)
    private int interactionCount7d;
    private int interactionCount30d;
    private double affinityScore;

    // Temporal (interaction-derived)
    private double lastInteractionHours;

    // NOTE: ctr and skipRate intentionally omitted until impression tracking is accurate
}
