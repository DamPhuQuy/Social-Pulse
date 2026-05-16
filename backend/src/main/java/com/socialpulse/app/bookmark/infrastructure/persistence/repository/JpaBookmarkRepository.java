package com.socialpulse.app.bookmark.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.bookmark.infrastructure.persistence.entity.BookmarkEntity;

@Repository
public interface JpaBookmarkRepository extends JpaRepository<BookmarkEntity, Long> {
    Optional<BookmarkEntity> findByUserIdAndPostId(Long userId, Long postId);

    Page<BookmarkEntity> findByUserIdOrderByCreatedAtDesc(Long userId, Pageable pageable);
}
