package com.socialpulse.app.post.application.service;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.feed.application.dto.response.OriginalPostData;
import com.socialpulse.app.post.application.dto.response.UserPostResponse;
import com.socialpulse.app.post.domain.enums.PostType;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class PostSummaryAssembler {
    private final UserRepository userRepository;
    private final PostReactionsRepository postReactionsRepository;
    private final PostRepository postRepository;

    public PostSummaryAssembler(UserRepository userRepository, PostReactionsRepository postReactionsRepository, PostRepository postRepository) {
        this.userRepository = userRepository;
        this.postReactionsRepository = postReactionsRepository;
        this.postRepository = postRepository;
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

        // ── Collect parent post IDs for SHARE-type posts ──────────────────
        Set<Long> parentPostIds = posts.stream()
                .filter(p -> p.getType() == PostType.SHARE && p.getParentPostId() != null)
                .map(Post::getParentPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Post> parentPostById = parentPostIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : postRepository.findByIds(parentPostIds).stream()
                        .filter(p -> p.getDeletedAt() == null)
                        .collect(Collectors.toMap(Post::getId, java.util.function.Function.identity()));

        // Merge all author IDs (post authors + parent post authors)
        Set<Long> allAuthorIds = new java.util.HashSet<>(authorIds);
        parentPostById.values().stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .forEach(allAuthorIds::add);

        Map<Long, User> userMap = userRepository.findByIds(allAuthorIds).stream()
                .collect(Collectors.toMap(User::getId, user -> user));

        Set<Long> postIds = posts.stream()
                .map(Post::getId)
                .collect(Collectors.toSet());

        Map<Long, ReactionType> reactionByPostId = postReactionsRepository
                .findByUserIdAndPostIds(viewerUserId, postIds)
                .stream()
                .collect(Collectors.toMap(PostReactions::getPostId, PostReactions::getReactionType));

        return posts.stream()
                .map(post -> toUserPostResponse(post, userMap.get(post.getUserId()), reactionByPostId.get(post.getId()), parentPostById, userMap))
                .toList();
    }

    private UserPostResponse toUserPostResponse(Post post, User author, ReactionType myReaction,
            Map<Long, Post> parentPostById, Map<Long, User> userMap) {
        if (author == null) {
            throw new AppException(UserCode.USER_NOT_FOUND);
        }

        // Build embedded original-post snapshot for SHARE-type posts
        OriginalPostData originalPost = null;
        if (post.getType() == PostType.SHARE && post.getParentPostId() != null) {
            Post parent = parentPostById.get(post.getParentPostId());
            if (parent != null) {
                User parentAuthor = userMap.get(parent.getUserId());
                originalPost = OriginalPostData.builder()
                        .postId(parent.getId())
                        .content(parent.getContent())
                        .imageUrl(parent.getImageUrl())
                        .topicSlugs(parent.getTopicSlugs())
                        .userId(parent.getUserId())
                        .username(parentAuthor != null ? parentAuthor.getUsername() : null)
                        .userAvatar(parentAuthor != null && parentAuthor.getProfile() != null
                                ? parentAuthor.getProfile().getAvatarUrl() : null)
                        .createdAt(parent.getCreatedAt())
                        .build();
            }
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
                .originalPost(originalPost)
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
