package com.socialpulse.app.feed.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.feed.domain.model.FeedImpression;

public interface JpaFeedImpressionRepository extends JpaRepository<FeedImpression, Long> {

    @Query("SELECT fi FROM FeedImpression fi WHERE fi.userId = :userId AND fi.postId = :postId ORDER BY fi.impressionTime DESC LIMIT 1")
    Optional<FeedImpression> findMostRecentImpression(@Param("userId") Long userId, @Param("postId") Long postId);

    @Query("SELECT fi FROM FeedImpression fi WHERE fi.interacted = false AND fi.impressionTime >= :cutoffTime")
    List<FeedImpression> findNonInteractedImpressionsSince(@Param("cutoffTime") LocalDateTime cutoffTime);
}
