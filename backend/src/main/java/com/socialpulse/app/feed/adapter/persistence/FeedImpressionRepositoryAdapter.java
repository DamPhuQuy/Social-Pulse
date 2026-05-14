package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Component;

import com.socialpulse.app.feed.domain.model.FeedImpression;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;
import com.socialpulse.app.feed.infrastructure.persistence.repository.JpaFeedImpressionRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class FeedImpressionRepositoryAdapter implements FeedImpressionRepository {

    private final JpaFeedImpressionRepository jpaRepository;

    @Override
    public FeedImpression save(FeedImpression impression) {
        return jpaRepository.save(impression);
    }

    @Override
    public Optional<FeedImpression> findMostRecentImpression(Long userId, Long postId) {
        return jpaRepository.findMostRecentImpression(userId, postId);
    }

    @Override
    public List<FeedImpression> findNonInteractedImpressionsSince(LocalDateTime cutoffTime) {
        return jpaRepository.findNonInteractedImpressionsSince(cutoffTime);
    }
}
