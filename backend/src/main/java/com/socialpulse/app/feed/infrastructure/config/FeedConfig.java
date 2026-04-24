package com.socialpulse.app.feed.infrastructure.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.jdbc.core.JdbcTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.adapter.persistence.FeedRepositoryAdapter;
import com.socialpulse.app.feed.application.service.AiRankingService;
import com.socialpulse.app.feed.application.service.CandidateSelectionService;
import com.socialpulse.app.feed.application.service.FeatureExtractionService;
import com.socialpulse.app.feed.application.service.FeedCacheService;
import com.socialpulse.app.feed.application.service.FeedRankingService;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.repository.FeedRepository;

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
    public ExtractFeaturesUseCase extractFeaturesUseCase(RedisTemplate<String, Object> redisTemplate) {
        return new FeatureExtractionService(redisTemplate);
    }

    @Bean
    public PredictRankingUseCase predictRankingUseCase(
            @Value("${ai.service.url:http://localhost:8001}") String aiServiceUrl) {
        return new AiRankingService(aiServiceUrl);
    }

    @Bean
    public CacheFeedUseCase cacheFeedUseCase(RedisTemplate<String, Object> redisTemplate) {
        ObjectMapper objectMapper = new ObjectMapper();
        objectMapper.findAndRegisterModules();
        return new FeedCacheService(redisTemplate, objectMapper);
    }

    @Bean
    public RankFeedUseCase rankFeedUseCase(
            SelectCandidatesUseCase selectCandidatesUseCase,
            ExtractFeaturesUseCase extractFeaturesUseCase,
            PredictRankingUseCase predictRankingUseCase,
            CacheFeedUseCase cacheFeedUseCase) {
        return new FeedRankingService(
                selectCandidatesUseCase,
                extractFeaturesUseCase,
                predictRankingUseCase,
                cacheFeedUseCase);
    }
}
