package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.stereotype.Repository;

import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;
import com.socialpulse.app.feed.infrastructure.persistence.entity.FeedImpressionEntity;
import com.socialpulse.app.feed.infrastructure.persistence.repository.JpaFeedImpressionRepository;

@Repository
public class FeedImpressionRepositoryAdapter implements FeedImpressionRepository {
    private final JpaFeedImpressionRepository jpaFeedImpressionRepository;

    public FeedImpressionRepositoryAdapter(JpaFeedImpressionRepository jpaFeedImpressionRepository) {
        this.jpaFeedImpressionRepository = jpaFeedImpressionRepository;
    }

    @Override
    public void saveAll(Long viewerId, List<FeedItem> feedItems, int page, int size, String feedContext) {
        if (viewerId == null || feedItems == null || feedItems.isEmpty()) {
            return;
        }

        int startRank = Math.max(page, 0) * Math.max(size, 0);
        List<FeedImpressionEntity> entities = new ArrayList<>(feedItems.size());

        for (int i = 0; i < feedItems.size(); i++) {
            FeedItem item = feedItems.get(i);
            FeedImpressionEntity entity = FeedImpressionEntity.builder()
                    .viewerId(viewerId)
                    .postId(item.getPostId())
                    .rankPosition(startRank + i)
                    .pageNumber(page)
                    .pageSize(size)
                    .rankingScore(item.getRankingScore())
                    .candidateSource(item.getSource() != null ? item.getSource().name() : null)
                    .rankingProvider(item.getRankingProvider() != null ? item.getRankingProvider().name() : "FALLBACK")
                    .featureSchemaVersion(item.getFeatureSchemaVersion())
                    .feedContext(feedContext)
                    .createdAt(LocalDateTime.now())
                    .build();
            entities.add(entity);
        }

        jpaFeedImpressionRepository.saveAll(entities);
    }

    @Override
    public long countAll() {
        return jpaFeedImpressionRepository.countAllImpressions();
    }

    @Override
    public long countByCreatedAtAfter(LocalDateTime since) {
        if (since == null) {
            return countAll();
        }
        return jpaFeedImpressionRepository.countByCreatedAtAfter(since);
    }

    @Override
    public long countByRankingProviderSince(String rankingProvider, LocalDateTime since) {
        if (since == null) {
            return jpaFeedImpressionRepository.countByRankingProvider(rankingProvider);
        }
        return jpaFeedImpressionRepository.countByRankingProviderAndCreatedAtAfter(rankingProvider, since);
    }
}
