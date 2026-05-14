package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.dto.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.UserFeatures;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts feed-ranking features from static post, author, and follow data.
 *
 * <p>Pushshift-only recommender scope does not use live user-behavior signals.</p>
 */
@Slf4j
public class FeatureExtractionService implements ExtractFeaturesUseCase {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final FollowRepository followRepository;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public FeatureExtractionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            FollowRepository followRepository,
            UserRepository userRepository,
            PostRepository postRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.followRepository = followRepository;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
    }

    @Override
    public List<RankingFeatures> extractFeatures(Long viewerId, List<CandidatePost> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        // === PHASE 1: Collect all unique IDs ===
        Set<Long> authorIds = candidates.stream()
                .map(c -> c.getPost().getUserId())
                .collect(Collectors.toSet());
        Set<Long> allUserIds = new HashSet<>(authorIds);
        allUserIds.add(viewerId);

        Map<Long, User> userMap = userRepository.findByIds(allUserIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> postCountMap = postRepository.countByUserIds(allUserIds);
        Set<Long> followedAuthorIds = followRepository.findFollowedUserIds(viewerId, authorIds);

        log.debug("Feature extraction batch queries: {} users, {} authors, {} follows",
                userMap.size(), authorIds.size(), followedAuthorIds.size());

        UserFeatures viewerFeatures = buildUserFeatures(viewerId, userMap, postCountMap);
        Map<Long, InteractionFeatures> interactionMap = buildInteractionMap(viewerId, authorIds, followedAuthorIds);

        // === PHASE 4: Assemble per-post features (pure computation, no DB) ===
        return candidates.stream().map(candidate -> {
            Post post = candidate.getPost();
            Long authorId = post.getUserId();

            return RankingFeatures.builder()
                    .postId(post.getId())
                    .postFeatures(extractPostFeatures(post, now))
                    .authorFeatures(buildUserFeatures(authorId, userMap, postCountMap))
                    .viewerFeatures(viewerFeatures)
                    .interactionFeatures(interactionMap.get(authorId))
                    .build();
        }).toList();
    }

    // === Post Features — pure computation on Post domain object ===

    private PostFeatures extractPostFeatures(Post post, LocalDateTime now) {
        long up = post.getUpvoteCount() != null ? post.getUpvoteCount() : 0L;
        long down = post.getDownvoteCount() != null ? post.getDownvoteCount() : 0L;
        double upvoteRatio = (up + down) > 0 ? (double) up / (up + down) : 0.5;

        double postAgeHours = post.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0
                : 0.0;

        return PostFeatures.builder()
                .postId(post.getId())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .cmtCount(post.getCmtCount())
                .viewCount(post.getViewCount())
                .shareCount(post.getShareCount())
                .hotScore(post.getHotScore())
                .upvoteRatio(upvoteRatio)
                .hasImage(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .build();
    }

    // === User Features — from batch-queried maps ===

    private UserFeatures buildUserFeatures(Long userId, Map<Long, User> userMap, Map<Long, Long> postCountMap) {
        // Try cache first
        String cacheKey = "user:features:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserFeatures.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached user features for userId={}", userId, e);
            }
        }

        User user = userMap.get(userId);
        long postCount = postCountMap.getOrDefault(userId, 0L);

        long accountAgeDays = 0L;
        if (user != null && user.getCreatedAt() != null) {
            accountAgeDays = ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now());
        }

        UserFeatures features = UserFeatures.builder()
                .userId(userId)
                .engagementRate(0.0) // Requires interaction data, default to 0 for now
                .postCount(postCount)
                .accountAgeDays(accountAgeDays)
                .build();

        // Cache for 10 minutes
        try {
            String json = objectMapper.writeValueAsString(features);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
        } catch (Exception e) {
            log.warn("Failed to cache user features for userId={}", userId, e);
        }

        return features;
    }

    private Map<Long, InteractionFeatures> buildInteractionMap(
            Long viewerId,
            Set<Long> authorIds,
            Set<Long> followedAuthorIds) {
        return authorIds.stream().collect(Collectors.toMap(
                authorId -> authorId,
                authorId -> buildInteractionForAuthor(viewerId, authorId, followedAuthorIds.contains(authorId))
        ));
    }

    private InteractionFeatures buildInteractionForAuthor(
            Long viewerId,
            Long authorId,
            boolean isFollowingAuthor) {
        return InteractionFeatures.builder()
                .viewerId(viewerId)
                .authorId(authorId)
                .interactionCount7d(0)
                .interactionCount30d(0)
                .affinityScore(isFollowingAuthor ? 2.0 : 0.0)
                .lastInteractionHours(999.0)
                .build();
    }
}
