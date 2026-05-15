package com.socialpulse.app.feed.application.dto;

import java.util.List;

import com.socialpulse.app.ai.lightgbm.LightGbmFeatureSchema;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingRequest {
    @Builder.Default
    private String featureSchemaVersion = LightGbmFeatureSchema.DEFAULT_SCHEMA_VERSION;
    private List<RankingFeatures> features;
}
