package com.socialpulse.app.cloudinary.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "cloudinary")
public class CloudinaryProperties {
    private String url;
    private String cloudName;
    private String apiKey;
    private String apiSecret;
}

