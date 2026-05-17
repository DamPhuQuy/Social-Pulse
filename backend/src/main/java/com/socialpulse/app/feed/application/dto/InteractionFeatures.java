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
    private Long interactionCount7d;
    private Long interactionCount30d;
    private Double hoursSinceLastInteraction;
    private Double affinityScore;
}
