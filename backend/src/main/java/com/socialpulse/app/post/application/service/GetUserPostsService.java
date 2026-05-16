package com.socialpulse.app.post.application.service;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.dto.response.PageResponse;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.application.usecase.GetUserPostsUseCase;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class GetUserPostsService implements GetUserPostsUseCase {

    private final PostRepository postRepository;
    private final UserRepository userRepository;

    public GetUserPostsService(PostRepository postRepository, UserRepository userRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public PageResponse<UserPostResponse> getUserPosts(Long userId, int page, int size, CustomUserDetails currentUser) {
        User author = userRepository.findById(userId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        var pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createdAt"));
        boolean canViewPrivate = userId.equals(currentUser.getId()) || hasPostManagePermission(currentUser);

        Page<Post> posts = canViewPrivate
                ? postRepository.findActiveByUserId(userId, pageable)
                : postRepository.findActiveByUserIdAndPrivacy(userId, Privacy.PUBLIC, pageable);

        return PageResponse.<UserPostResponse>builder()
                .items(posts.getContent().stream()
                        .map(post -> toUserPostResponse(post, author))
                        .toList())
                .page(posts.getNumber())
                .size(posts.getSize())
                .totalElements(posts.getTotalElements())
                .totalPages(posts.getTotalPages())
                .hasNext(posts.hasNext())
                .build();
    }

    private boolean hasPostManagePermission(CustomUserDetails currentUser) {
        return currentUser.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("post:manage"));
    }

    private UserPostResponse toUserPostResponse(Post post, User author) {
        return UserPostResponse.builder()
                .id(post.getId())
                .parentPostId(post.getParentPostId())
                .type(post.getType())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .userId(author.getId())
                .username(author.getUsername())
                .userAvatar(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                .privacy(post.getPrivacy())
                .upvoteCount(post.getUpvoteCount() == null ? 0L : post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount() == null ? 0L : post.getDownvoteCount())
                .cmtCount(post.getCmtCount() == null ? 0L : post.getCmtCount())
                .shareCount(post.getShareCount() == null ? 0L : post.getShareCount())
                .createdAt(post.getCreatedAt())
                .build();
    }
}
