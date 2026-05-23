package com.socialpulse.app.admin.application.dto;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiStatusResponse {
    private boolean enabled;
    private String baseUrl;
    private String featureSchemaVersion;
    private boolean healthReachable;
    private boolean modelAvailable;
    private boolean modelLoaded;
    private boolean trainingControlsAvailable;
}
