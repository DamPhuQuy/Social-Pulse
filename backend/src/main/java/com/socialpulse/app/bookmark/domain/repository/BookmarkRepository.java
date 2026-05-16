package com.socialpulse.app.bookmark.domain.repository;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.bookmark.domain.model.Bookmark;

public interface BookmarkRepository {
    Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId);

    Bookmark save(Bookmark bookmark);

    void delete(Bookmark bookmark);

    Page<Bookmark> findByUserId(Long userId, Pageable pageable);
}
