package com.socialpulse.app.feed.application.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRelationshipFeatures {
    private Long userId;
    private Long authorId;
    private Integer interactionCount7d;
    private Integer interactionCount30d;
    private Double hoursSinceLastInteraction;
    private Double affinityScore;
}
