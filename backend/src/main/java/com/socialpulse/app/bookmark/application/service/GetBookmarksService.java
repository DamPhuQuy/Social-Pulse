package com.socialpulse.app.bookmark.application.service;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import com.socialpulse.app.bookmark.application.usecase.GetBookmarksUseCase;
import com.socialpulse.app.bookmark.domain.model.Bookmark;
import com.socialpulse.app.bookmark.domain.repository.BookmarkRepository;
import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.service.PostSummaryAssembler;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;

@Service
public class GetBookmarksService implements GetBookmarksUseCase {
    private final BookmarkRepository bookmarkRepository;
    private final PostRepository postRepository;
    private final PostSummaryAssembler postSummaryAssembler;

    public GetBookmarksService(
            BookmarkRepository bookmarkRepository,
            PostRepository postRepository,
            PostSummaryAssembler postSummaryAssembler) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.postSummaryAssembler = postSummaryAssembler;
    }

    @Override
    public PageResponse<UserPostResponse> getBookmarks(int page, int size, CustomUserDetails currentUser) {
        Page<Bookmark> bookmarks = bookmarkRepository.findByUserId(currentUser.getId(), PageRequest.of(page, size));

        List<Long> orderedPostIds = bookmarks.getContent().stream()
                .map(Bookmark::getPostId)
                .toList();
        Set<Long> postIds = Set.copyOf(orderedPostIds);
        Map<Long, Post> postMap = postRepository.findByIds(postIds).stream()
                .filter(post -> post.getDeletedAt() == null)
                .collect(Collectors.toMap(Post::getId, post -> post));

        List<Post> orderedPosts = orderedPostIds.stream()
                .map(postMap::get)
                .filter(post -> post != null)
                .toList();

        return PageResponse.<UserPostResponse>builder()
                .items(postSummaryAssembler.assemble(orderedPosts))
                .page(bookmarks.getNumber())
                .size(bookmarks.getSize())
                .totalElements(bookmarks.getTotalElements())
                .totalPages(bookmarks.getTotalPages())
                .hasNext(bookmarks.hasNext())
                .build();
    }
}
