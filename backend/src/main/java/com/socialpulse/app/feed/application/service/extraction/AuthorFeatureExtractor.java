package com.socialpulse.app.feed.application.service.extraction;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.Map;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.dto.features.support.AuthorFeatures;
import com.socialpulse.app.user.domain.model.User;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AuthorFeatureExtractor {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public AuthorFeatureExtractor(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    public AuthorFeatures extract(Long authorId, Map<Long, User> userMap,
                                  Map<Long, Long> postCountMap, Map<Long, Double> avgPopularityMap) {
        String cacheKey = "author:features:" + authorId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AuthorFeatures.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached author features for authorId={}", authorId, e);
            }
        }

        User user = userMap.get(authorId);
        long postCount = postCountMap.getOrDefault(authorId, 0L);
        double avgPopularity = avgPopularityMap.getOrDefault(authorId, 0.0);
        double seniorityYears = user != null && user.getCreatedAt() != null
                ? Math.max(ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now()), 0L) / 365.0
                : 0.0;

        AuthorFeatures features = AuthorFeatures.builder()
                .authorId(authorId)
                .seniorityYears(seniorityYears)
                .postCount(postCount)
                .averagePopularity(avgPopularity)
                .build();

        try {
            redisTemplate.opsForValue().set(cacheKey, objectMapper.writeValueAsString(features), Duration.ofMinutes(10));
        } catch (Exception e) {
            log.warn("Failed to cache author features for authorId={}", authorId, e);
        }
        return features;
    }
}
