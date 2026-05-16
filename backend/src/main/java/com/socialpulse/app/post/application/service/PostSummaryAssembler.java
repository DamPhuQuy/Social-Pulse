package com.socialpulse.app.post.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class PostSummaryAssembler {
    private final UserRepository userRepository;

    public PostSummaryAssembler(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    public List<UserPostResponse> assemble(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        Set<Long> authorIds = posts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        return posts.stream()
                .map(post -> toUserPostResponse(post, userMap.get(post.getUserId())))
                .toList();
    }

    private UserPostResponse toUserPostResponse(Post post, User author) {
        if (author == null) {
            throw new AppException(UserCode.USER_NOT_FOUND);
        }

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
