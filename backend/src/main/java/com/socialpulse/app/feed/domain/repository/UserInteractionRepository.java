package com.socialpulse.app.feed.domain.repository;

import java.time.LocalDateTime;

public interface UserInteractionRepository {
    long countByViewerAndAuthorSince(Long viewerId, Long authorId, LocalDateTime since);

    LocalDateTime findLatestInteractionTime(Long viewerId, Long authorId);

    long countTotalByViewerSince(Long viewerId, LocalDateTime since);

    void save(Long viewerId, Long authorId, String interactionType);

    java.util.Map<Long, com.socialpulse.app.feed.domain.model.UserInteractionAggregate> findAggregatesByViewerAndAuthors(
            Long viewerId, java.util.Set<Long> authorIds, LocalDateTime since30d, LocalDateTime since7d);
}
