package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import com.socialpulse.app.feed.domain.model.UserInteractionAggregate;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.feed.infrastructure.persistence.entity.UserInteractionEntity;
import com.socialpulse.app.feed.infrastructure.persistence.repository.JpaUserInteractionRepository;

public class UserInteractionRepositoryAdapter implements UserInteractionRepository {
    private final JpaUserInteractionRepository jpaUserInteractionRepository;

    public UserInteractionRepositoryAdapter(JpaUserInteractionRepository jpaUserInteractionRepository) {
        this.jpaUserInteractionRepository = jpaUserInteractionRepository;
    }

    @Override
    public long countByViewerAndAuthorSince(Long viewerId, Long authorId, LocalDateTime since) {
        return jpaUserInteractionRepository.countByViewerAndAuthorSince(viewerId, authorId, since);
    }

    @Override
    public LocalDateTime findLatestInteractionTime(Long viewerId, Long authorId) {
        return jpaUserInteractionRepository.findLatestInteractionTime(viewerId, authorId);
    }

    @Override
    public long countTotalByViewerSince(Long viewerId, LocalDateTime since) {
        return jpaUserInteractionRepository.countTotalByViewerSince(viewerId, since);
    }

    @Override
    public void save(Long viewerId, Long authorId, String interactionType) {
        UserInteractionEntity entity = UserInteractionEntity.builder()
                .viewerId(viewerId)
                .authorId(authorId)
                .interactionType(interactionType)
                .createdAt(LocalDateTime.now())
                .build();
        jpaUserInteractionRepository.save(entity);
    }

    @Override
    public Map<Long, UserInteractionAggregate> findAggregatesByViewerAndAuthors(
            Long viewerId, Set<Long> authorIds, LocalDateTime since30d, LocalDateTime since7d) {
        if (authorIds == null || authorIds.isEmpty()) {
            return Map.of();
        }

        List<Object[]> results = jpaUserInteractionRepository.findAggregatesByViewerAndAuthors(
                viewerId, authorIds, since30d, since7d);

        Map<Long, UserInteractionAggregate> map = new HashMap<>();
        for (Object[] row : results) {
            Long authorId = (Long) row[0];
            long count7d = row[1] != null ? ((Number) row[1]).longValue() : 0L;
            long count30d = row[2] != null ? ((Number) row[2]).longValue() : 0L;
            LocalDateTime latest = (LocalDateTime) row[3];

            map.put(authorId, new UserInteractionAggregate(authorId, count7d, count30d, latest));
        }
        return map;
    }
}
