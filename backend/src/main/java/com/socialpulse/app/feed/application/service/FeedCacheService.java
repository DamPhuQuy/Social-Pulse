package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.domain.model.FeedItem;

@Service
public class FeedCacheService implements CacheFeedUseCase {
    private final RedisTemplate<String, Object> redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String FEED_CACHE_PREFIX = "user:feed:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public FeedCacheService(RedisTemplate<String, Object> redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void cacheFeed(Long userId, List<FeedItem> feedItems) {
        String key = FEED_CACHE_PREFIX + userId;
        try {
            String json = objectMapper.writeValueAsString(feedItems);
            redisTemplate.opsForValue().set(key, json, CACHE_TTL);
        } catch (Exception e) {
            // Log error but don't fail
        }
    }

    @Override
    public List<FeedItem> getCachedFeed(Long userId) {
        String key = FEED_CACHE_PREFIX + userId;
        try {
            String json = (String) redisTemplate.opsForValue().get(key);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<FeedItem>>() {});
            }
        } catch (Exception e) {
            // Log error but don't fail
        }
        return null;
    }

    @Override
    public void invalidateFeed(Long userId) {
        String key = FEED_CACHE_PREFIX + userId;
        redisTemplate.delete(key);
    }
}
