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
    @Builder.Default
    private String featureSchemaVersion = "v1";
    private List<RankingFeatures> features;
}
