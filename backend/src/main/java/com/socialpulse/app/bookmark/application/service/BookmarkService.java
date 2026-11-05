package com.socialpulse.app.bookmark.application.service;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.bookmark.infrastructure.persistence.entity.BookmarkEntity;
import com.socialpulse.app.bookmark.infrastructure.persistence.repository.JpaBookmarkRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.post.infrastructure.persistence.entity.PostEntity;
import com.socialpulse.app.post.infrastructure.persistence.repository.JpaPostRepository;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class BookmarkService {

    private final JpaBookmarkRepository bookmarkRepository;
    private final JpaPostRepository postRepository;
    private final JpaUserRepository userRepository;

    public BookmarkService(
            JpaBookmarkRepository bookmarkRepository,
            JpaPostRepository postRepository,
            JpaUserRepository userRepository) {
        this.bookmarkRepository = bookmarkRepository;
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Transactional
    public boolean toggleBookmark(Long postId, Long userId) {
        Optional<BookmarkEntity> existing = bookmarkRepository.findByUserIdAndPostId(userId, postId);
        if (existing.isPresent()) {
            bookmarkRepository.delete(existing.get());
            log.debug("User {} removed bookmark on post {}", userId, postId);
            return false;
        } else {
            PostEntity post = postRepository.findById(postId)
                    .filter(p -> p.getDeletedAt() == null)
                    .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

            UserEntity user = userRepository.findById(userId)
                    .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

            BookmarkEntity bookmark = BookmarkEntity.builder()
                    .post(post)
                    .user(user)
                    .build();

            bookmarkRepository.save(bookmark);
            log.debug("User {} bookmarked post {}", userId, postId);
            return true;
        }
    }

    @Transactional(readOnly = true)
    public boolean isBookmarked(Long postId, Long userId) {
        if (userId == null || postId == null) return false;
        return bookmarkRepository.existsByUserIdAndPostId(userId, postId);
    }

    @Transactional(readOnly = true)
    public List<FeedItemResponse> getMyBookmarks(Long userId, int page, int size) {
        Page<Long> postIdsPage = bookmarkRepository.findBookmarkedPostIdsByUserId(userId, PageRequest.of(page, size));
        if (postIdsPage.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> postIds = postIdsPage.getContent();
        List<PostEntity> posts = postRepository.findAllByIdIn(Set.copyOf(postIds));
        Map<Long, PostEntity> postMap = posts.stream().collect(Collectors.toMap(PostEntity::getId, p -> p));

        return postIds.stream()
                .map(postMap::get)
                .filter(p -> p != null && p.getDeletedAt() == null)
                .map(p -> FeedItemResponse.builder()
                        .postId(p.getId())
                        .parentPostId(p.getParentPostId())
                        .type(p.getType())
                        .title(p.getTitle())
                        .sourceUrl(p.getSourceUrl())
                        .sourceDomain(p.getSourceDomain())
                        .summary(p.getSummary())
                        .isBookmarked(true)
                        .content(p.getContent())
                        .imageUrl(p.getImageUrl())
                        .topicSlugs(p.getTopicSlugs())
                        .userId(p.getUser().getId())
                        .username(p.getUser().getUsername())
                        .userAvatar(p.getUser().getProfile() != null ? p.getUser().getProfile().getAvatarUrl() : null)
                        .upvoteCount(p.getUpvoteCount() != null ? p.getUpvoteCount() : 0L)
                        .downvoteCount(p.getDownvoteCount() != null ? p.getDownvoteCount() : 0L)
                        .cmtCount(p.getCmtCount() != null ? p.getCmtCount() : 0L)
                        .shareCount(p.getShareCount() != null ? p.getShareCount() : 0L)
                        .privacy(p.getPrivacy())
                        .createdAt(p.getCreatedAt())
                        .updatedAt(p.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
