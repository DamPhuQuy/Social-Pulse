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
import com.socialpulse.app.feed.application.dto.response.OriginalPostData;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.post.domain.enums.PostType;
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

        // ── Collect parent post IDs for SHARE-type posts ──────────────────
        Set<Long> parentPostIds = postById.values().stream()
                .filter(p -> p.getType() == PostType.SHARE && p.getParentPostId() != null)
                .map(Post::getParentPostId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<Long, Post> parentPostById = parentPostIds.isEmpty()
                ? java.util.Collections.emptyMap()
                : postRepository.findByIds(parentPostIds).stream()
                        .filter(p -> p.getDeletedAt() == null)   // show even if private/toxic
                        .collect(Collectors.toMap(Post::getId, Function.identity()));

        // Collect all author IDs (sharer authors + original post authors)
        Set<Long> allAuthorIds = new java.util.HashSet<>(authorIds);
        parentPostById.values().stream()
                .map(Post::getUserId)
                .filter(Objects::nonNull)
                .forEach(allAuthorIds::add);

        Map<Long, User> userById = userRepository.findByIds(allAuthorIds).stream()
                .collect(Collectors.toMap(User::getId, Function.identity()));

        Map<Long, ReactionType> reactionByPostId = viewerUserId != null
                ? postReactionsRepository.findByUserIdAndPostIds(viewerUserId, postIds).stream()
                        .collect(Collectors.toMap(PostReactions::getPostId, PostReactions::getReactionType))
                : java.util.Collections.emptyMap();

        return feedItems.stream()
                .map(item -> toResponse(item, postById.get(item.getPostId()), userById, reactionByPostId, parentPostById))
                .filter(Objects::nonNull)
                .toList();
    }

    private FeedItemResponse toResponse(
            FeedItem item,
            Post post,
            Map<Long, User> userById,
            Map<Long, ReactionType> reactionByPostId,
            Map<Long, Post> parentPostById) {
        if (post == null) {
            return null;
        }

        User author = userById.get(post.getUserId());
        if (author == null) {
            return null;
        }

        // Build embedded original-post snapshot for SHARE-type items
        OriginalPostData originalPost = null;
        if (post.getType() == PostType.SHARE && post.getParentPostId() != null) {
            Post parent = parentPostById.get(post.getParentPostId());
            if (parent != null) {
                User parentAuthor = userById.get(parent.getUserId());
                originalPost = OriginalPostData.builder()
                        .postId(parent.getId())
                        .content(parent.getContent())
                        .imageUrl(parent.getImageUrl())
                        .topicSlugs(parent.getTopicSlugs())
                        .userId(parent.getUserId())
                        .username(parentAuthor != null ? parentAuthor.getUsername() : null)
                        .userAvatar(parentAuthor != null ? extractAvatar(parentAuthor) : null)
                        .createdAt(parent.getCreatedAt())
                        .build();
            }
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
                .rankingScore(item.getRankingScore())
                .source(item.getSource() != null ? item.getSource().name() : null)
                .rankingProvider(item.getRankingProvider() != null ? item.getRankingProvider().name() : null)
                .featureSchemaVersion(item.getFeatureSchemaVersion())
                .rankedAt(item.getRankedAt())
                .affinityScore(item.getAffinityScore())
                .interactionCount30d(item.getInteractionCount30d())
                .privacy(post.getPrivacy())
                .createdAt(post.getCreatedAt())
                .updatedAt(post.getUpdatedAt())
                .originalPost(originalPost)
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
