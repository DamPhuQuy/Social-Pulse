package com.socialpulse.app.feed.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.feed.infrastructure.persistence.entity.UserInteractionEntity;

public interface JpaUserInteractionRepository extends JpaRepository<UserInteractionEntity, Long> {

    @Query("SELECT COUNT(u) FROM UserInteractionEntity u WHERE u.viewerId = :viewerId AND u.authorId = :authorId AND u.createdAt >= :since")
    long countByViewerAndAuthorSince(
            @Param("viewerId") Long viewerId,
            @Param("authorId") Long authorId,
            @Param("since") LocalDateTime since);

    @Query(value = """
            SELECT created_at
            FROM user_interactions
            WHERE viewer_id = :viewerId AND author_id = :authorId
            ORDER BY created_at DESC
            LIMIT 1
            """, nativeQuery = true)
    LocalDateTime findLatestInteractionTime(
            @Param("viewerId") Long viewerId,
            @Param("authorId") Long authorId);

    @Query("SELECT COUNT(u) FROM UserInteractionEntity u WHERE u.viewerId = :viewerId AND u.createdAt >= :since")
    long countTotalByViewerSince(
            @Param("viewerId") Long viewerId,
            @Param("since") LocalDateTime since);

    @Query("""
            SELECT u.authorId,
                   SUM(CASE WHEN u.createdAt >= :since7d THEN 1L ELSE 0L END),
                   SUM(CASE WHEN u.createdAt >= :since30d THEN 1L ELSE 0L END),
                   MAX(u.createdAt)
            FROM UserInteractionEntity u
            WHERE u.viewerId = :viewerId AND u.authorId IN :authorIds
            GROUP BY u.authorId
            """)
    List<Object[]> findAggregatesByViewerAndAuthors(
            @Param("viewerId") Long viewerId,
            @Param("authorIds") Set<Long> authorIds,
            @Param("since30d") LocalDateTime since30d,
            @Param("since7d") LocalDateTime since7d);
}
