package com.socialpulse.app.feed.application.usecase.ranking;

import java.util.List;

import com.socialpulse.app.feed.domain.model.FeedItem;

public interface RankFeedUseCase {
    List<FeedItem> getRankedFeed(Long userId);
    List<FeedItem> getPaginatedFeed(Long userId, int page, int size);
    List<FeedItem> getPaginatedFeed(Long userId, int page, int size, String topicSlug);
}
