package com.socialpulse.app.admin.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.admin.application.service.GetSystemMetricsService;
import com.socialpulse.app.admin.application.usecase.GetSystemMetricsUseCase;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class AdminConfig {

    @Bean
    public GetSystemMetricsUseCase getSystemMetricsUseCase(
            UserRepository userRepository,
            PostRepository postRepository) {
        return new GetSystemMetricsService(userRepository, postRepository);
    }
}
