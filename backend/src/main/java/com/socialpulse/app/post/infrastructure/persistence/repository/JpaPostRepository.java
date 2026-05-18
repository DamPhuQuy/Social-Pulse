package com.socialpulse.app.post.infrastructure.persistence.repository;

import java.util.List;
import java.util.Set;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;

@Repository
public interface JpaPostRepository extends JpaRepository<PostEntity, Long> {

    // get posts by user id
    Page<PostEntity> findByUserId(Long userId, Pageable pageable);

    Page<PostEntity> findByUserIdAndDeletedAtIsNull(Long userId, Pageable pageable);

    Page<PostEntity> findByUserIdAndPrivacyAndDeletedAtIsNull(Long userId, Privacy privacy, Pageable pageable);

    List<PostEntity> findAllByIdIn(Set<Long> ids);

    boolean existsByUserIdAndParentPostIdAndType(Long userId, Long parentPostId, PostType type);

    @Query("UPDATE PostEntity p SET p.shareCount = p.shareCount + :delta WHERE p.id = :postId")
    void updateShareCount(@Param("postId") Long postId, @Param("delta") Long delta);

    long countByUserId(Long userId);

    @Query("SELECT p.user.id, COUNT(p) FROM PostEntity p WHERE p.user.id IN :userIds GROUP BY p.user.id")
    List<Object[]> countByUserIds(@Param("userIds") java.util.Set<Long> userIds);

    @Query("""
            SELECT p.user.id,
                   AVG(COALESCE(p.upvoteCount, 0) + COALESCE(p.cmtCount, 0) + COALESCE(p.shareCount, 0))
            FROM PostEntity p
            WHERE p.user.id IN :userIds
            GROUP BY p.user.id
            """)
    List<Object[]> averagePopularityByUserIds(@Param("userIds") Set<Long> userIds);

    @Query("""
            SELECT p
            FROM PostEntity p
            WHERE p.deletedAt IS NULL
              AND p.privacy = com.socialpulse.app.post.domain.enums.Privacy.PUBLIC
              AND p.toxic = false
              AND LOWER(COALESCE(p.content, '')) LIKE LOWER(CONCAT('%', :query, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<PostEntity> searchPublicActiveByContent(@Param("query") String query, Pageable pageable);

    @Query("""
            SELECT p
            FROM PostEntity p
            WHERE p.deletedAt IS NULL
              AND p.privacy = com.socialpulse.app.post.domain.enums.Privacy.PUBLIC
              AND p.toxic = false
              AND LOWER(COALESCE(p.content, '')) LIKE LOWER(CONCAT('%', :hashtag, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<PostEntity> findPublicActiveByHashtag(@Param("hashtag") String hashtag, Pageable pageable);

    @Query("""
            SELECT p
            FROM PostEntity p
            WHERE p.deletedAt IS NULL
              AND p.privacy = com.socialpulse.app.post.domain.enums.Privacy.PUBLIC
              AND p.toxic = false
              AND LOWER(COALESCE(p.content, '')) LIKE LOWER(CONCAT('%', :mention, '%'))
            ORDER BY p.createdAt DESC
            """)
    Page<PostEntity> findPublicActiveByMention(@Param("mention") String mention, Pageable pageable);

    @Query("""
            SELECT p
            FROM PostEntity p
            WHERE p.deletedAt IS NULL
              AND p.privacy = com.socialpulse.app.post.domain.enums.Privacy.PUBLIC
              AND p.toxic = false
              AND p.createdAt >= :since
            ORDER BY p.createdAt DESC
            """)
    List<PostEntity> findRecentPublicActiveSince(@Param("since") LocalDateTime since);

    long countByCreatedAtAfter(LocalDateTime since);
    long countByToxicTrue();
    long countByDeletedAtAfter(LocalDateTime since);
}
