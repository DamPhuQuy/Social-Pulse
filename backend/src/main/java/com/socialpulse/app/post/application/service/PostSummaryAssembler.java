package com.socialpulse.app.post.application.service;

import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class PostSummaryAssembler {
    private final UserRepository userRepository;
    private final PostReactionsRepository postReactionsRepository;

    public PostSummaryAssembler(UserRepository userRepository, PostReactionsRepository postReactionsRepository) {
        this.userRepository = userRepository;
        this.postReactionsRepository = postReactionsRepository;
    }

    public List<UserPostResponse> assemble(List<Post> posts) {
        return assemble(posts, null);
    }

    public List<UserPostResponse> assemble(List<Post> posts, Long viewerUserId) {
        if (posts == null || posts.isEmpty()) {
            return List.of();
        }

        Set<Long> authorIds = posts.stream()
                .map(Post::getUserId)
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Set<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        Map<Long, ReactionType> reactionByPostId = postReactionsRepository
                .findByUserIdAndPostIds(viewerUserId, postIds)
                .stream()
                .collect(Collectors.toMap(PostReactions::getPostId, PostReactions::getReactionType));

        return posts.stream()
                .map(post -> toUserPostResponse(post, userMap.get(post.getUserId()), reactionByPostId.get(post.getId())))
                .toList();
    }

    private UserPostResponse toUserPostResponse(Post post, User author, ReactionType myReaction) {
        if (author == null) {
            throw new AppException(UserCode.USER_NOT_FOUND);
        }

        return UserPostResponse.builder()
                .id(post.getId())
                .postId(post.getId())
                .parentPostId(post.getParentPostId())
                .type(post.getType())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .topicSlugs(post.getTopicSlugs())
                .userId(author.getId())
                .username(author.getUsername())
                .userAvatar(author.getProfile() != null ? author.getProfile().getAvatarUrl() : null)
                .privacy(post.getPrivacy())
                .upvoteCount(post.getUpvoteCount() == null ? 0L : post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount() == null ? 0L : post.getDownvoteCount())
                .cmtCount(post.getCmtCount() == null ? 0L : post.getCmtCount())
                .shareCount(post.getShareCount() == null ? 0L : post.getShareCount())
                .myReaction(myReaction)
                .myVote(toVote(myReaction))
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private int toVote(ReactionType reactionType) {
        if (reactionType == ReactionType.UPVOTE) {
            return 1;
        }
        if (reactionType == ReactionType.DOWNVOTE) {
            return -1;
        }
        return 0;
    }
}
