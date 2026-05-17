package com.socialpulse.app.feed.application.dto.features.core;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.socialpulse.app.feed.application.dto.features.support.AuthorFeatures;
import com.socialpulse.app.feed.application.dto.features.support.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.features.support.PostFeatures;

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
