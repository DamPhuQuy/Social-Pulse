package com.socialpulse.app.infrastructure.cloudinary.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@Data
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    private String url;
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}

