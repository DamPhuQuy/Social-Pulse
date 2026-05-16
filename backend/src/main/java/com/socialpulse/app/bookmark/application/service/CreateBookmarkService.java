package com.socialpulse.app.bookmark.application.service;

import com.socialpulse.app.bookmark.application.dto.response.BookmarkResponse;
import com.socialpulse.app.bookmark.application.usecase.CreateBookmarkUseCase;
import com.socialpulse.app.bookmark.domain.model.Bookmark;
import com.socialpulse.app.bookmark.domain.repository.BookmarkRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

public class CreateBookmarkService implements CreateBookmarkUseCase {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final BookmarkResponseMapper bookmarkResponseMapper;

    public CreateBookmarkService(
            BookmarkRepository bookmarkRepository,
            PostRepository postRepository,
            BookmarkResponseMapper bookmarkResponseMapper) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.bookmarkResponseMapper = bookmarkResponseMapper;
    }

    @Override
    public BookmarkResponse createBookmark(Long postId, CustomUserDetails currentUser) {
        Bookmark existing = bookmarkRepository.findByUserIdAndPostId(currentUser.getId(), postId).orElse(null);
        if (existing != null) {
            return bookmarkResponseMapper.toResponse(existing);
        }

        Post post = postRepository.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        boolean canView = post.getPrivacy() == Privacy.PUBLIC
                || post.getUserId().equals(currentUser.getId())
                || currentUser.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals("post:manage"));
        if (!canView) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }

        Bookmark savedBookmark = bookmarkRepository.save(Bookmark.builder()
                .userId(currentUser.getId())
                .postId(postId)
                .build());
        return bookmarkResponseMapper.toResponse(savedBookmark);
    }
}
