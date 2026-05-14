package com.socialpulse.app.feed.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.adapter.persistence.FeedRepositoryAdapter;
import com.socialpulse.app.feed.application.service.AiRankingService;
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
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class FeedConfig {

    @Bean
    public FeedRepository feedRepository(JdbcTemplate jdbcTemplate) {
        return new FeedRepositoryAdapter(jdbcTemplate);
    }

    @Bean
    public SelectCandidatesUseCase selectCandidatesUseCase(FeedRepository feedRepository) {
        return new CandidateSelectionService(feedRepository);
    }

    @Bean
    public ExtractFeaturesUseCase extractFeaturesUseCase(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            FollowRepository followRepository,
            UserRepository userRepository,
            PostRepository postRepository) {
        return new FeatureExtractionService(
                redisTemplate, objectMapper,
                followRepository, userRepository,
                postRepository);
    }

    @Bean
    public PredictRankingUseCase predictRankingUseCase(
            @Value("${ai.service.url:http://localhost:5000}") String aiServiceUrl,
            @Value("${ai.service.enabled:false}") boolean aiServiceEnabled,
            @Value("${ai.service.timeout-ms:1500}") int timeoutMs,
            @Value("${ai.service.ranking-path:/rank}") String rankingPath) {
        return new AiRankingService(aiServiceUrl, aiServiceEnabled, timeoutMs, rankingPath);
    }

    @Bean
    public FallbackRankingService fallbackRankingService() {
        return new FallbackRankingService();
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
            FallbackRankingService fallbackRankingService) {
        return new FeedRankingService(
                selectCandidatesUseCase,
                extractFeaturesUseCase,
                predictRankingUseCase,
                cacheFeedUseCase,
                fallbackRankingService);
    }
}

