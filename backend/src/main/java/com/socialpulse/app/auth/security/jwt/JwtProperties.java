package com.socialpulse.app.auth.security.jwt;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import lombok.Data;

@ConfigurationProperties(prefix = "app.jwt")
@Component
@Data
public class JwtProperties {
    private String secret;
    private long expirationMs;
    private long refreshExpirationMs;
}
