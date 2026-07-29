package com.socialpulse.app.feed.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;

public interface JpaFeedPostRepository extends JpaRepository<PostEntity, Long> {

    @Query(value = """
            SELECT * FROM posts
            WHERE deleted_at IS NULL
              AND privacy = 'PUBLIC'
              AND toxic = false
              AND created_at >= :since
            ORDER BY created_at DESC, id DESC
            """, nativeQuery = true)
    List<PostEntity> findRecentPosts(@Param("since") LocalDateTime since, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM posts p
            INNER JOIN follows f ON p.user_id = f.following_id
            WHERE f.follower_id = :userId
              AND p.deleted_at IS NULL
              AND p.privacy = 'PUBLIC'
              AND p.toxic = false
              AND p.created_at >= :since
            ORDER BY p.created_at DESC, p.id DESC
            """, nativeQuery = true)
    List<PostEntity> findFollowingPosts(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    @Query(value = """
            SELECT p.* FROM posts p
            WHERE p.deleted_at IS NULL
              AND p.privacy = 'PUBLIC'
              AND p.toxic = false
              AND p.created_at >= :since
              AND (
                EXISTS (SELECT 1 FROM follows f WHERE f.follower_id = :userId AND f.following_id = p.user_id)
                OR
                EXISTS (
                  SELECT 1 FROM topic_follows tf
                  JOIN topics t ON tf.topic_slug = t.slug
                  WHERE tf.user_id = :userId AND t.id = p.topic_id
                )
              )
            ORDER BY p.created_at DESC, p.id DESC
            """, nativeQuery = true)
    List<PostEntity> findFollowingUserAndTopicPosts(
            @Param("userId") Long userId,
            @Param("since") LocalDateTime since,
            Pageable pageable);

    @Query(value = """
            SELECT * FROM posts
            WHERE deleted_at IS NULL
              AND privacy = 'PUBLIC'
              AND toxic = false
              AND created_at >= :since
            ORDER BY (upvote_count * 1.0 + cmt_count * 2.0 + share_count * 2.0) DESC, created_at DESC, id DESC
            """, nativeQuery = true)
    List<PostEntity> findPopularPosts(@Param("since") LocalDateTime since, Pageable pageable);

    @Query(value = """
            SELECT p.* FROM posts p
            LEFT JOIN topics t ON p.topic_id = t.id
            WHERE (t.slug = :normalizedSlug OR LOWER(t.name) = :normalizedSlug)
              AND p.deleted_at IS NULL
              AND p.privacy = 'PUBLIC'
              AND p.toxic = false
              AND p.created_at >= :since
            ORDER BY p.created_at DESC, p.id DESC
            """, nativeQuery = true)
    List<PostEntity> findByTopicSlug(
            @Param("normalizedSlug") String normalizedSlug,
            @Param("since") LocalDateTime since,
            Pageable pageable);
}
