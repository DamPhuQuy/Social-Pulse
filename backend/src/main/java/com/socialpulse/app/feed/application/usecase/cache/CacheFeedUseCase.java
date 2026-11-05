package com.socialpulse.app.feed.application.usecase.cache;

import java.util.List;

import com.socialpulse.app.feed.domain.model.FeedItem;

public interface CacheFeedUseCase {
    void cacheFeed(String cacheKey, List<FeedItem> feedItems);
    List<FeedItem> getCachedFeed(String cacheKey);
    void invalidateFeed(String cacheKey);

    boolean acquireLock(String lockKey, long timeoutMs);
    void releaseLock(String lockKey);

    default void cacheFeed(Long userId, List<FeedItem> feedItems) {
        if (userId != null) {
            cacheFeed("user:feed:" + userId, feedItems);
        }
    }

    default List<FeedItem> getCachedFeed(Long userId) {
        return userId != null ? getCachedFeed("user:feed:" + userId) : null;
    }

    default void invalidateFeed(Long userId) {
        if (userId != null) {
            invalidateFeed("user:feed:" + userId);
        }
    }
}
