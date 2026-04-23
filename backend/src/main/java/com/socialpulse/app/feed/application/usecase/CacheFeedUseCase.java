package com.socialpulse.app.feed.application.usecase;

import java.util.List;

import com.socialpulse.app.feed.domain.model.FeedItem;

public interface CacheFeedUseCase {
    void cacheFeed(Long userId, List<FeedItem> feedItems);
    List<FeedItem> getCachedFeed(Long userId);
    void invalidateFeed(Long userId);
}
