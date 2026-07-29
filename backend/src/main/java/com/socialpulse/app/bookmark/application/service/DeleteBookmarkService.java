package com.socialpulse.app.bookmark.application.service;
import org.springframework.stereotype.Service;

import com.socialpulse.app.bookmark.application.usecase.DeleteBookmarkUseCase;
import com.socialpulse.app.bookmark.domain.repository.BookmarkRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class DeleteBookmarkService implements DeleteBookmarkUseCase {
    private final BookmarkRepository bookmarkRepository;

    public DeleteBookmarkService(BookmarkRepository bookmarkRepository) {
        this.bookmarkRepository = bookmarkRepository;
    }

    @Override
    public void deleteBookmark(Long postId, CustomUserDetails currentUser) {
        bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId)
                .ifPresent(bookmarkRepository::delete);
    }
}
