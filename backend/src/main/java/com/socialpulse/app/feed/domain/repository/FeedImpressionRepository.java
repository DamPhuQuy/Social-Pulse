package com.socialpulse.app.feed.domain.repository;

import java.util.List;
import java.time.LocalDateTime;

import com.socialpulse.app.feed.domain.model.FeedItem;

public interface FeedImpressionRepository {
    void saveAll(Long viewerId, List<FeedItem> feedItems, int page, int size, String feedContext);

    long countAll();

    long countByCreatedAtAfter(LocalDateTime since);

    long countByRankingProviderSince(String rankingProvider, LocalDateTime since);
}
