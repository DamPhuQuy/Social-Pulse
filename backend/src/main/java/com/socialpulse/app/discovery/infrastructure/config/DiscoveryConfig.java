package com.socialpulse.app.discovery.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.discovery.application.service.GetPostsByHashtagService;
import com.socialpulse.app.discovery.application.service.GetPostsByMentionService;
import com.socialpulse.app.discovery.application.service.GetTrendingHashtagsService;
import com.socialpulse.app.discovery.application.service.SearchPostsService;
import com.socialpulse.app.discovery.application.service.SearchUsersService;
import com.socialpulse.app.discovery.application.usecase.GetPostsByHashtagUseCase;
import com.socialpulse.app.discovery.application.usecase.GetPostsByMentionUseCase;
import com.socialpulse.app.discovery.application.usecase.GetTrendingHashtagsUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchPostsUseCase;
import com.socialpulse.app.discovery.application.usecase.SearchUsersUseCase;
import com.socialpulse.app.feed.application.service.ContentAnalysisService;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class DiscoveryConfig {
    @Bean
    public SearchUsersUseCase searchUsersUseCase(UserRepository userRepository) {
        return new SearchUsersService(userRepository);
    }

    @Bean
    public SearchPostsUseCase searchPostsUseCase(PostRepository postRepository, PostSummaryAssembler postSummaryAssembler) {
        return new SearchPostsService(postRepository, postSummaryAssembler);
    }

    @Bean
    public GetPostsByHashtagUseCase getPostsByHashtagUseCase(
            PostRepository postRepository,
            PostSummaryAssembler postSummaryAssembler) {
        return new GetPostsByHashtagService(postRepository, postSummaryAssembler);
    }

    @Bean
    public GetPostsByMentionUseCase getPostsByMentionUseCase(
            PostRepository postRepository,
            PostSummaryAssembler postSummaryAssembler) {
        return new GetPostsByMentionService(postRepository, postSummaryAssembler);
    }

    @Bean
    public GetTrendingHashtagsUseCase getTrendingHashtagsUseCase(
            PostRepository postRepository,
            ContentAnalysisService contentAnalysisService) {
        return new GetTrendingHashtagsService(postRepository, contentAnalysisService);
    }
}
