package com.socialpulse.app.cloudinary.config;

import com.cloudinary.Cloudinary;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.HashMap;
import java.util.Map;

@Configuration
public class CloudinaryConfig {
    private final CloudinaryProperties cloudinaryProperties;

    public CloudinaryConfig(CloudinaryProperties cloudinaryProperties) {
        this.cloudinaryProperties = cloudinaryProperties;
    }

    @Bean
    public Cloudinary cloudinary() {
        if (!isBlank(cloudinaryProperties.getUrl())) {
            return new Cloudinary(cloudinaryProperties.getUrl());
        }

        if (isBlank(cloudinaryProperties.getCloudName())
                || isBlank(cloudinaryProperties.getApiKey())
                || isBlank(cloudinaryProperties.getApiSecret())) {
            throw new IllegalStateException("Cloudinary is not configured. Set CLOUDINARY_URL or CLOUDINARY_CLOUD_NAME, CLOUDINARY_API_KEY, CLOUDINARY_API_SECRET.");
        }

        Map<String, String> config = new HashMap<>();
        config.put("cloud_name", cloudinaryProperties.getCloudName());
        config.put("api_key", cloudinaryProperties.getApiKey());
        config.put("api_secret", cloudinaryProperties.getApiSecret());
        config.put("secure", "true");

        return new Cloudinary(config);
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }
}
