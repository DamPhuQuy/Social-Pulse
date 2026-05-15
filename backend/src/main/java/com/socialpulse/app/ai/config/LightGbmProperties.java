package com.socialpulse.app.ai.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@ConfigurationProperties(prefix = "ai.lightgbm")
public class LightGbmProperties {
    private boolean enabled = false;
    private String modelLocation = "classpath:ai/lightgbm-ranking-model.json";
    private String featureSchemaVersion = "v1";
}
