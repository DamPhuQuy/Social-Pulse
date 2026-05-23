package com.socialpulse.app.post.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.post.adapter.persistence.PostReactionsRepositoryAdapter;
import com.socialpulse.app.post.adapter.persistence.PostRepositoryAdapter;
import com.socialpulse.app.post.application.dto.mapper.PostMapper;
import com.socialpulse.app.post.application.service.CreatePostService;
import com.socialpulse.app.post.application.service.DeletePostService;
import com.socialpulse.app.post.application.service.EditPostService;
import com.socialpulse.app.post.application.service.GetUserPostsService;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.application.service.ReactPostService;
import com.socialpulse.app.post.application.service.ViewPostService;
import com.socialpulse.app.post.application.usecase.CreatePostUseCase;
import com.socialpulse.app.post.application.usecase.DeletePostUseCase;
import com.socialpulse.app.post.application.usecase.EditPostUseCase;
import com.socialpulse.app.post.application.usecase.GetUserPostsUseCase;
import com.socialpulse.app.post.application.usecase.ReactPostUseCase;
import com.socialpulse.app.post.application.usecase.ViewPostUseCase;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.infrastructure.persistence.mapper.PostPersistenceMapper;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostReactionRepository;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;
import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
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
                                               PostMapper postMapper,
                                               StringRedisTemplate redisTemplate,
                                               SseEmitterRegistry sseEmitterRegistry) {
        return new CreatePostService(postRepository, userRepository, postMapper, redisTemplate, sseEmitterRegistry);
    }

    @Bean
    public ViewPostUseCase viewPostUseCase(PostRepository postRepository,
                                           PostReactionsRepository postReactionsRepository,
                                           UserRepository userRepository,
                                           PostMapper postMapper) {
        return new ViewPostService(postRepository, postReactionsRepository, userRepository, postMapper);
    }

    @Bean
    public DeletePostUseCase deletePostUseCase(PostRepository postRepository, StringRedisTemplate redisTemplate, SseEmitterRegistry sseEmitterRegistry) {
        return new DeletePostService(postRepository, redisTemplate, sseEmitterRegistry);
    }

    @Bean
    public EditPostUseCase editPostUseCase(PostRepository postRepository, PostMapper postMapper, StringRedisTemplate redisTemplate, SseEmitterRegistry sseEmitterRegistry) {
        return new EditPostService(postRepository, postMapper, redisTemplate, sseEmitterRegistry);
    }

    @Bean
    public ReactPostUseCase reactPostUseCase(PostRepository postRepository,
                                             PostReactionsRepository postReactionsRepository,
                                             UserRepository userRepository,
                                             PostMapper postMapper,
                                             NotificationCommandService notificationCommandService,
                                             SseEmitterRegistry sseEmitterRegistry,
                                             UserInteractionRepository userInteractionRepository) {
        return new ReactPostService(postRepository, postReactionsRepository, userRepository,
                postMapper, notificationCommandService, sseEmitterRegistry, userInteractionRepository);
    }

    @Bean
    public GetUserPostsUseCase getUserPostsUseCase(
            PostRepository postRepository,
            UserRepository userRepository,
            PostSummaryAssembler postSummaryAssembler) {
        return new GetUserPostsService(postRepository, userRepository, postSummaryAssembler);
    }
}
