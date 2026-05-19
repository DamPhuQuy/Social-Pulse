package com.socialpulse.app.feed.application.service.assembler;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.feed.application.dto.response.FeedItemResponse;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.model.PostReactions;
import com.socialpulse.app.post.domain.repository.PostReactionsRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Service
public class FeedItemResponseAssembler {
    private final PostRepository postRepository;
    private final UserRepository userRepository;
    private final PostReactionsRepository postReactionsRepository;

    public FeedItemResponseAssembler(
            PostRepository postRepository,
            UserRepository userRepository,
            PostReactionsRepository postReactionsRepository) {
        this.postRepository = postRepository;
        this.userRepository = userRepository;
        this.postReactionsRepository = postReactionsRepository;
    }

    public List<FeedItemResponse> assemble(List<FeedItem> feedItems, Long viewerUserId) {
        if (feedItems == null || feedItems.isEmpty()) {
            return List.of();
        }

        Set<Long> postIds = feedItems.stream()
                .map(FeedItem::getPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Map<Long, Post> postById = postRepository.findByIds(postIds).stream()
                .filter(this::isVisibleInFeed)
                .collect(Collectors.toMap(Post::getId, Function.identity()));

        Set<Long> authorIds = postById.values().stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, User> userById = userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, ReactionType> reactionByPostId = postReactionsRepository
                .findByUserIdAndPostIds(viewerUserId, postIds).stream()
                .collect(Collectors.toMap(PostReactions::getPostId, PostReactions::getReactionType));

        return feedItems.stream()
                .map(item -> toResponse(item, postById.get(item.getPostId()), userById, reactionByPostId))
                .filter(Objects::nonNull)
                .toList();
    }

    private FeedItemResponse toResponse(
            FeedItem item,
            Post post,
            Map<Long, User> userById,
            Map<Long, ReactionType> reactionByPostId) {
        if (post == null) {
            return null;
        }

        User author = userById.get(post.getUserId());
        if (author == null) {
            return null;
        }

        ReactionType myReaction = reactionByPostId.get(post.getId());
        return FeedItemResponse.builder()
                .postId(post.getId())
                .parentPostId(post.getParentPostId())
                .type(post.getType())
                .content(post.getContent())
                .imageUrl(post.getImageUrl())
                .topicSlugs(post.getTopicSlugs())
                .userId(author.getId())
                .username(author.getUsername())
                .userAvatar(extractAvatar(author))
                .upvoteCount(safeCount(post.getUpvoteCount()))
                .downvoteCount(safeCount(post.getDownvoteCount()))
                .cmtCount(safeCount(post.getCmtCount()))
                .shareCount(safeCount(post.getShareCount()))
                .myReaction(myReaction)
                .myVote(toVote(myReaction))
                .aiScore(item.getAiScore())
                .source(item.getSource() != null ? item.getSource().name() : null)
                .rankedAt(item.getRankedAt())
                .privacy(post.getPrivacy())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .build();
    }

    private boolean isVisibleInFeed(Post post) {
        return post.getDeletedAt() == null
                && post.getPrivacy() == Privacy.PUBLIC
                && !post.isToxic();
    }

    private String extractAvatar(User user) {
        UserProfile profile = user.getProfile();
        return profile != null ? profile.getAvatarUrl() : null;
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
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
