package com.socialpulse.app.feed.application.dto.request;

import java.util.List;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;
import com.socialpulse.app.feed.application.dto.features.core.RankingFeatures;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class RankingRequest {
    @Builder.Default
    private String featureSchemaVersion = "v1";
    private List<RankingFeatures> features;
}
