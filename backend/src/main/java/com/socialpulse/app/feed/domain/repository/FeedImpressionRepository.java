package com.socialpulse.app.feed.domain.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import com.socialpulse.app.feed.domain.model.FeedImpression;

public interface FeedImpressionRepository {
    FeedImpression save(FeedImpression impression);
    Optional<FeedImpression> findMostRecentImpression(Long userId, Long postId);
    List<FeedImpression> findNonInteractedImpressionsSince(LocalDateTime cutoffTime);
}
