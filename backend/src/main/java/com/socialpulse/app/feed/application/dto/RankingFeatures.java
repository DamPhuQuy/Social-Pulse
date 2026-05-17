package com.socialpulse.app.feed.application.dto;

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
public class RankingFeatures {
    private Long postId;
    private PostFeatures postFeatures;
    private AuthorFeatures authorFeatures;
    private InteractionFeatures interactionFeatures;
}
