package com.socialpulse.app.feed.infrastructure.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.adapter.persistence.FeedRepositoryAdapter;
import com.socialpulse.app.feed.adapter.persistence.UserInteractionRepositoryAdapter;
import com.socialpulse.app.feed.application.service.cache.FeedCacheService;
import com.socialpulse.app.feed.application.service.candidate.CandidateSelectionService;
import com.socialpulse.app.feed.application.service.extraction.AuthorFeatureExtractor;
import com.socialpulse.app.feed.application.service.extraction.FeatureExtractionService;
import com.socialpulse.app.feed.application.service.extraction.InteractionFeatureExtractor;
import com.socialpulse.app.feed.application.service.extraction.PostFeatureExtractor;
import com.socialpulse.app.feed.application.service.ranking.FallbackRankingService;
import com.socialpulse.app.feed.application.service.ranking.FeedRankingService;
import com.socialpulse.app.feed.application.service.ranking.ScoreBoostService;
import com.socialpulse.app.feed.application.usecase.cache.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.candidate.SelectCandidatesUseCase;
import com.socialpulse.app.feed.application.usecase.extraction.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.RankFeedUseCase;
import com.socialpulse.app.block.JpaBlockRepository;
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
    public UserInteractionRepository userInteractionRepository(com.socialpulse.app.feed.infrastructure.persistence.repository.JpaUserInteractionRepository jpaUserInteractionRepository) {
        return new UserInteractionRepositoryAdapter(jpaUserInteractionRepository);
    }

    @Bean
    public SelectCandidatesUseCase selectCandidatesUseCase(
            FeedRepository feedRepository, 
            StringRedisTemplate redisTemplate,
            JpaBlockRepository blockRepository) {
        return new CandidateSelectionService(feedRepository, redisTemplate, blockRepository);
    }

    @Bean
    public PostFeatureExtractor postFeatureExtractor() {
        return new PostFeatureExtractor();
    }

    @Bean
    public AuthorFeatureExtractor authorFeatureExtractor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        return new AuthorFeatureExtractor(redisTemplate, objectMapper);
    }

    @Bean
    public InteractionFeatureExtractor interactionFeatureExtractor(UserInteractionRepository userInteractionRepository) {
        return new InteractionFeatureExtractor(userInteractionRepository);
    }

    @Bean
    public ExtractFeaturesUseCase extractFeaturesUseCase(
            UserRepository userRepository,
            PostRepository postRepository,
            UserInteractionRepository userInteractionRepository,
            PostFeatureExtractor postFeatureExtractor,
            AuthorFeatureExtractor authorFeatureExtractor,
            InteractionFeatureExtractor interactionFeatureExtractor) {
        return new FeatureExtractionService(
                userRepository, postRepository, userInteractionRepository,
                postFeatureExtractor, authorFeatureExtractor, interactionFeatureExtractor);
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
    public ScoreBoostService scoreBoostService() {
        return new ScoreBoostService();
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
            ScoreBoostService scoreBoostService,
            AiPipelineProperties properties) {
        return new FeedRankingService(
                selectCandidatesUseCase, extractFeaturesUseCase, predictRankingUseCase,
                cacheFeedUseCase, fallbackRankingService, scoreBoostService,
                properties.getFeatureSchemaVersion());
    }
}
