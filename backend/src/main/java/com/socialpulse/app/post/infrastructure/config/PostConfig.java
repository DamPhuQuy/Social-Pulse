package com.socialpulse.app.post.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.post.adapter.out.PostReactionsRepositoryAdapter;
import com.socialpulse.app.post.adapter.out.PostRepositoryAdapter;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.dto.mapper.PostReactionMapper;
import com.socialpulse.app.post.application.port.in.CreatePostUseCase;
import com.socialpulse.app.post.application.port.in.ReactPostUseCase;
import com.socialpulse.app.post.application.port.in.ViewPostUseCase;
import com.socialpulse.app.post.application.port.out.PostReactionsRepositoryPort;
import com.socialpulse.app.post.application.port.out.PostRepositoryPort;
import com.socialpulse.app.post.application.service.CreatePostService;
import com.socialpulse.app.post.application.service.ReactPostService;
import com.socialpulse.app.post.application.service.ViewPostService;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostDomainToEntity;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostEntityToDomain;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostReactionRepository;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;

@Configuration
public class PostConfig {

    // adapters --------------------------------------

    @Bean
    public PostRepositoryPort postRepositoryPort(JpaPostRepository jpaPostRepository,
                                                 PostEntityToDomain postEntityToDomain,
                                                 PostDomainToEntity postDomainToEntity) {
        return new PostRepositoryAdapter(jpaPostRepository, postEntityToDomain, postDomainToEntity);
    }

    @Bean
    public PostReactionsRepositoryPort postReactionsRepositoryPort(
            JpaPostReactionRepository jpaPostReactionRepository,
            PostEntityToDomain postEntityToDomain,
            PostDomainToEntity postDomainToEntity) {
        return new PostReactionsRepositoryAdapter(jpaPostReactionRepository, postEntityToDomain, postDomainToEntity);
    }

    // use cases --------------------------------------

    @Bean
    public CreatePostUseCase createPostUseCase(PostRepositoryPort postRepositoryPort,
                                               UserRepositoryPort userRepositoryPort,
                                               PostMapper postMapper) {
        return new CreatePostService(postRepositoryPort, userRepositoryPort, postMapper);
    }

    @Bean
    public ViewPostUseCase viewPostUseCase(PostRepositoryPort postRepositoryPort,
                                           PostMapper postMapper) {
        return new ViewPostService(postRepositoryPort, postMapper);
    }

    @Bean
    public ReactPostUseCase reactPostUseCase(PostRepositoryPort postRepositoryPort,
                                             PostReactionsRepositoryPort postReactionsRepositoryPort,
                                             UserRepositoryPort userRepositoryPort,
                                             PostReactionMapper postReactionMapper) {
        return new ReactPostService(postRepositoryPort, postReactionsRepositoryPort, userRepositoryPort,
                postReactionMapper);
    }
}

