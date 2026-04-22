package com.socialpulse.app.post.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.post.adapter.persistence.PostReactionsRepositoryAdapter;
import com.socialpulse.app.post.adapter.persistence.PostRepositoryAdapter;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.service.CreatePostService;
import com.socialpulse.app.post.application.service.ReactPostService;
import com.socialpulse.app.post.application.service.ViewPostService;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.application.usecase.ReactPostUseCase;
import com.socialpulse.app.post.application.usecase.ViewPostUseCase;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostPersistenceMapper;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostReactionRepository;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class PostConfig {

    // adapters --------------------------------------

    @Bean
    public PostRepository postRepository(JpaPostRepository jpaPostRepository,
                                                 PostPersistenceMapper postPersistenceMapper) {
        return new PostRepositoryAdapter(jpaPostRepository, postPersistenceMapper);
    }

    @Bean
    public PostReactionsRepository postReactionsRepository(
            JpaPostReactionRepository jpaPostReactionRepository,
            PostPersistenceMapper postPersistenceMapper) {
        return new PostReactionsRepositoryAdapter(jpaPostReactionRepository, postPersistenceMapper);
    }

    // use cases --------------------------------------

    @Bean
    public CreatePostUseCase createPostUseCase(PostRepository postRepository,
                                               UserRepository userRepository,
                                               PostMapper postMapper) {
        return new CreatePostService(postRepository, userRepository, postMapper);
    }

    @Bean
    public ViewPostUseCase viewPostUseCase(PostRepository postRepository,
                                           PostMapper postMapper) {
        return new ViewPostService(postRepository, postMapper);
    }

    @Bean
    public ReactPostUseCase reactPostUseCase(PostRepository postRepository,
                                             PostReactionsRepository postReactionsRepository,
                                             UserRepository userRepository,
                                             PostMapper postMapper) {
        return new ReactPostService(postRepository, postReactionsRepository, userRepository,
                postMapper);
    }
}



