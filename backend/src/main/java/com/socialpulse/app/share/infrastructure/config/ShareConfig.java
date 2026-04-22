package com.socialpulse.app.share.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.share.application.service.SharePostService;
import com.socialpulse.app.share.application.usecase.ShareUseCase;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class ShareConfig {

    @Bean
    public ShareUseCase shareUseCase(PostRepository postRepository, UserRepository userRepository) {
        return new SharePostService(postRepository, userRepository);
    }
}
