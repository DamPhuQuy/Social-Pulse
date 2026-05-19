package com.socialpulse.app.realtime.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;

@Configuration
public class RealtimeConfig {

    @Bean
    public SseEmitterRegistry sseEmitterRegistry() {
        return new SseEmitterRegistry();
    }
}
