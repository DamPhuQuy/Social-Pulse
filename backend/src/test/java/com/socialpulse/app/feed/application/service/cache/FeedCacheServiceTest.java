package com.socialpulse.app.feed.application.service.cache;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Duration;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.domain.model.FeedItem;

@ExtendWith(MockitoExtension.class)
class FeedCacheServiceTest {

    @Mock private StringRedisTemplate redisTemplate;
    @Mock private ValueOperations<String, String> valueOperations;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private FeedCacheService feedCacheService;

    @BeforeEach
    void setUp() {
        feedCacheService = new FeedCacheService(redisTemplate, objectMapper);
    }

    @Test
    @DisplayName("cacheFeed stores serialized JSON in Redis with 10-minute TTL")
    void cacheFeed_storesInRedisWithTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);

        List<FeedItem> items = List.of(FeedItem.builder().postId(1L).userId(10L).build());
        String cacheKey = "feed:global:p0:s20";

        feedCacheService.cacheFeed(cacheKey, items);

        verify(valueOperations).set(eq(cacheKey), anyString(), eq(Duration.ofMinutes(10)));
    }

    @Test
    @DisplayName("getCachedFeed returns deserialized items on cache hit")
    void getCachedFeed_hit_returnsItems() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cacheKey = "feed:global:p0:s20";
        String json = "[{\"postId\":1,\"userId\":10}]";

        when(valueOperations.get(cacheKey)).thenReturn(json);

        List<FeedItem> result = feedCacheService.getCachedFeed(cacheKey);

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals(1L, result.get(0).getPostId());
    }

    @Test
    @DisplayName("getCachedFeed returns null on cache miss")
    void getCachedFeed_miss_returnsNull() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String cacheKey = "feed:global:p0:s20";

        when(valueOperations.get(cacheKey)).thenReturn(null);

        List<FeedItem> result = feedCacheService.getCachedFeed(cacheKey);

        assertNull(result);
    }

    @Test
    @DisplayName("acquireLock uses SET NX PX for atomic singleflight stampede guard")
    void acquireLock_usesSetNxPx() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        String lockKey = "stampede:feed:global";

        when(valueOperations.setIfAbsent(eq("lock:" + lockKey), eq("1"), eq(Duration.ofMillis(5000))))
                .thenReturn(true);

        boolean acquired = feedCacheService.acquireLock(lockKey, 5000);

        assertTrue(acquired);
        verify(valueOperations).setIfAbsent(eq("lock:" + lockKey), eq("1"), eq(Duration.ofMillis(5000)));
    }

    @Test
    @DisplayName("releaseLock deletes the singleflight lock key")
    void releaseLock_deletesKey() {
        String lockKey = "stampede:feed:global";

        feedCacheService.releaseLock(lockKey);

        verify(redisTemplate).delete("lock:" + lockKey);
    }
}
