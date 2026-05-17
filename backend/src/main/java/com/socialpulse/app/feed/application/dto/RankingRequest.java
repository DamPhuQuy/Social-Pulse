package com.socialpulse.app.feed.application.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingRequest {
    public static final String DEFAULT_SCHEMA_VERSION = "v1";

    @Builder.Default
    private String featureSchemaVersion = DEFAULT_SCHEMA_VERSION;
    private List<RankingFeatures> features;
}
