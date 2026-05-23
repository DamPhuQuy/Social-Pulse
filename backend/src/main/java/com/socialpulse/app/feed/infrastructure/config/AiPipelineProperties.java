package com.socialpulse.app.feed.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.pipeline")
public class AiPipelineProperties {
    private boolean enabled = false;
    private String baseUrl = "http://localhost:8000";
    private String featureSchemaVersion = "v2";
}
