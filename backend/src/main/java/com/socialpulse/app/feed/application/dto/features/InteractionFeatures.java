package com.socialpulse.app.feed.application.dto.features;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class InteractionFeatures {
    private Long interactionCount7d;
    private Long interactionCount30d;
    private Double hoursSinceLastInteraction;
    private Double affinityScore;
}
