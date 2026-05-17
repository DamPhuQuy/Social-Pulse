package com.socialpulse.app.feed.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.adapter.persistence.FeedRepositoryAdapter;
import com.socialpulse.app.feed.adapter.persistence.UserInteractionRepositoryAdapter;
import com.socialpulse.app.feed.application.service.CandidateSelectionService;
import com.socialpulse.app.feed.application.service.FallbackRankingService;
import com.socialpulse.app.feed.application.service.FeatureExtractionService;
import com.socialpulse.app.feed.application.service.FeedCacheService;
import com.socialpulse.app.feed.application.service.FeedRankingService;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.repository.FeedRepository;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
@EnableConfigurationProperties(AiPipelineProperties.class)
public class FeedConfig {

    @Bean
    public FeedRepository feedRepository(JdbcTemplate jdbcTemplate) {
        return new FeedRepositoryAdapter(jdbcTemplate);
    }

    @Bean
    public UserInteractionRepository userInteractionRepository(JdbcTemplate jdbcTemplate) {
        return new UserInteractionRepositoryAdapter(jdbcTemplate);
    }

    @Bean
    public SelectCandidatesUseCase selectCandidatesUseCase(FeedRepository feedRepository) {
        return new CandidateSelectionService(feedRepository);
    }

    @Bean
    public ExtractFeaturesUseCase extractFeaturesUseCase(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PostRepository postRepository,
            UserInteractionRepository userInteractionRepository) {
        return new FeatureExtractionService(
                redisTemplate, objectMapper,
                userRepository,
                postRepository,
                userInteractionRepository);
    }

    @Bean
    public PredictRankingUseCase predictRankingUseCase(AiPipelineProperties properties) {
        return new AiPipelineRankingClient(properties.getBaseUrl(), properties.isEnabled());
    }

    @Bean
    public FallbackRankingService fallbackRankingService(AiPipelineProperties properties) {
        return new FallbackRankingService(properties.getFeatureSchemaVersion());
    }

    @Bean
    public CacheFeedUseCase cacheFeedUseCase(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new FeedCacheService(redisTemplate, objectMapper);
    }

    @Bean
    public RankFeedUseCase rankFeedUseCase(
            SelectCandidatesUseCase selectCandidatesUseCase,
            ExtractFeaturesUseCase extractFeaturesUseCase,
            PredictRankingUseCase predictRankingUseCase,
            CacheFeedUseCase cacheFeedUseCase,
            FallbackRankingService fallbackRankingService,
            AiPipelineProperties properties) {
        return new FeedRankingService(
                selectCandidatesUseCase,
                extractFeaturesUseCase,
                predictRankingUseCase,
                cacheFeedUseCase,
                fallbackRankingService,
                properties.getFeatureSchemaVersion());
    }
}
