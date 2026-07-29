package com.socialpulse.app.feed.infrastructure.persistence.repository;

import java.time.LocalDateTime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.feed.infrastructure.persistence.entity.FeedImpressionEntity;

public interface JpaFeedImpressionRepository extends JpaRepository<FeedImpressionEntity, Long> {

    @Query("SELECT COUNT(f) FROM FeedImpressionEntity f")
    long countAllImpressions();

    @Query("SELECT COUNT(f) FROM FeedImpressionEntity f WHERE f.createdAt >= :since")
    long countByCreatedAtAfter(@Param("since") LocalDateTime since);

    @Query("SELECT COUNT(f) FROM FeedImpressionEntity f WHERE f.rankingProvider = :rankingProvider")
    long countByRankingProvider(@Param("rankingProvider") String rankingProvider);

    @Query("SELECT COUNT(f) FROM FeedImpressionEntity f WHERE f.rankingProvider = :rankingProvider AND f.createdAt >= :since")
    long countByRankingProviderAndCreatedAtAfter(
            @Param("rankingProvider") String rankingProvider,
            @Param("since") LocalDateTime since);
}
