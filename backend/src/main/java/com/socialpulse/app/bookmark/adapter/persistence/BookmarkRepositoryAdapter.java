package com.socialpulse.app.bookmark.adapter.persistence;

import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.socialpulse.app.bookmark.domain.model.Bookmark;
import com.socialpulse.app.bookmark.domain.repository.BookmarkRepository;
import com.socialpulse.app.bookmark.infrastructure.persistence.mapper.BookmarkPersistenceMapper;
import com.socialpulse.app.bookmark.infrastructure.persistence.repository.JpaBookmarkRepository;

public class BookmarkRepositoryAdapter implements BookmarkRepository {
    private final JpaBookmarkRepository jpaBookmarkRepository;
    private final BookmarkPersistenceMapper bookmarkPersistenceMapper;

    public BookmarkRepositoryAdapter(
            JpaBookmarkRepository jpaBookmarkRepository,
            BookmarkPersistenceMapper bookmarkPersistenceMapper) {
        this.jpaBookmarkRepository = jpaBookmarkRepository;
        this.bookmarkPersistenceMapper = bookmarkPersistenceMapper;
    }

    @Override
    public Optional<Bookmark> findByUserIdAndPostId(Long userId, Long postId) {
        return jpaBookmarkRepository.findByUserIdAndPostId(userId, postId)
                .map(bookmarkPersistenceMapper::toDomain);
    }

    @Override
    public Bookmark save(Bookmark bookmark) {
        return bookmarkPersistenceMapper.toDomain(jpaBookmarkRepository.save(bookmarkPersistenceMapper.toEntity(bookmark)));
    }

    @Override
    public void delete(Bookmark bookmark) {
        jpaBookmarkRepository.delete(bookmarkPersistenceMapper.toEntity(bookmark));
    }

    @Override
    public Page<Bookmark> findByUserId(Long userId, Pageable pageable) {
        return jpaBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId, pageable)
                .map(bookmarkPersistenceMapper::toDomain);
    }
}
