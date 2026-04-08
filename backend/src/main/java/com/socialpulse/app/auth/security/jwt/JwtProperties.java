package com.socialpulse.app.auth.security.jwt;

import lombok.AccessLevel;
import lombok.Data;
import lombok.Getter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.sql.Blob;

@ConfigurationProperties(prefix = "app.jwt")
@Component
@Data
public class JwtProperties {
    private String secret;

    private long expirationMs;
}
