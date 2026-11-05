package com.socialpulse.app.bookmark.infrastructure.persistence.repository;

import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.bookmark.infrastructure.persistence.entity.BookmarkEntity;

@Repository
public interface JpaBookmarkRepository extends JpaRepository<BookmarkEntity, Long> {
    Optional<BookmarkEntity> findByUserIdAndPostId(Long userId, Long postId);
    boolean existsByUserIdAndPostId(Long userId, Long postId);
    void deleteByUserIdAndPostId(Long userId, Long postId);

    @Query("SELECT b.post.id FROM BookmarkEntity b WHERE b.user.id = :userId ORDER BY b.createdAt DESC")
    Page<Long> findBookmarkedPostIdsByUserId(@Param("userId") Long userId, Pageable pageable);

    @Query("SELECT b.post.id FROM BookmarkEntity b WHERE b.user.id = :userId AND b.post.id IN :postIds")
    Set<Long> findBookmarkedPostIdsIn(@Param("userId") Long userId, @Param("postIds") Iterable<Long> postIds);
}
