package com.socialpulse.app.feed.application.service.cache;

import java.time.Duration;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.usecase.cache.CacheFeedUseCase;
import com.socialpulse.app.feed.domain.model.FeedItem;

@Service
public class FeedCacheService implements CacheFeedUseCase {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    private static final String FEED_CACHE_PREFIX = "user:feed:";
    private static final Duration CACHE_TTL = Duration.ofMinutes(10);

    public FeedCacheService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public void cacheFeed(String cacheKey, List<FeedItem> feedItems) {
        if (cacheKey == null || feedItems == null) return;
        try {
            String json = objectMapper.writeValueAsString(feedItems);
            redisTemplate.opsForValue().set(cacheKey, json, CACHE_TTL);
        } catch (Exception e) {
            // Non-fatal cache write failure
        }
    }

    @Override
    public List<FeedItem> getCachedFeed(String cacheKey) {
        if (cacheKey == null) return null;
        try {
            String json = redisTemplate.opsForValue().get(cacheKey);
            if (json != null) {
                return objectMapper.readValue(json, new TypeReference<List<FeedItem>>() {});
            }
        } catch (Exception e) {
            // Non-fatal cache read failure
        }
        return null;
    }

    @Override
    public void invalidateFeed(String cacheKey) {
        if (cacheKey == null) return;
        try {
            redisTemplate.delete(cacheKey);
        } catch (Exception e) {
            // Non-fatal cache invalidation failure
        }
    }

    @Override
    public boolean acquireLock(String lockKey, long timeoutMs) {
        if (lockKey == null) return false;
        try {
            Boolean acquired = redisTemplate.opsForValue().setIfAbsent(
                "lock:" + lockKey,
                "1",
                Duration.ofMillis(timeoutMs)
            );
            return Boolean.TRUE.equals(acquired);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public void releaseLock(String lockKey) {
        if (lockKey == null) return;
        try {
            redisTemplate.delete("lock:" + lockKey);
        } catch (Exception ignored) {
        }
    }

    @Override
    public void cacheFeed(Long userId, List<FeedItem> feedItems) {
        if (userId == null) return;
        cacheFeed(FEED_CACHE_PREFIX + userId, feedItems);
    }

    @Override
    public List<FeedItem> getCachedFeed(Long userId) {
        if (userId == null) return null;
        return getCachedFeed(FEED_CACHE_PREFIX + userId);
    }

    @Override
    public void invalidateFeed(Long userId) {
        if (userId == null) return;
        invalidateFeed(FEED_CACHE_PREFIX + userId);
    }
}
