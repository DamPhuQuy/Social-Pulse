package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.dto.features.AuthorFeatures;
import com.socialpulse.app.feed.application.dto.features.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.features.PostFeatures;
import com.socialpulse.app.feed.application.dto.features.RankingFeatures;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeatureExtractionService implements ExtractFeaturesUseCase {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PostRepository postRepository;
    private final UserInteractionRepository userInteractionRepository;

    public FeatureExtractionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PostRepository postRepository,
            UserInteractionRepository userInteractionRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
        this.userInteractionRepository = userInteractionRepository;
    }

    @Override
    public List<RankingFeatures> extractFeatures(Long viewerId, List<CandidatePost> candidates) {
        if (candidates.isEmpty()) {
            return List.of();
        }

        LocalDateTime now = LocalDateTime.now();

        Set<Long> authorIds = candidates.stream()
                .map(c -> c.getPost().getUserId())
                .collect(Collectors.toSet());

        Map<Long, User> userMap = userRepository.findByIds(authorIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));
        Map<Long, Long> postCountMap = postRepository.countByUserIds(authorIds);
        Map<Long, Double> averagePopularityMap = postRepository.averagePopularityByUserIds(authorIds);

        // Compute viewer's total interactions for affinity normalization
        long viewerTotalInteractions = userInteractionRepository.countTotalByViewerSince(viewerId, now.minusDays(30));

        return candidates.stream().map(candidate -> {
            Post post = candidate.getPost();
            Long authorId = post.getUserId();

            return RankingFeatures.builder()
                    .postId(post.getId())
                    .postFeatures(extractPostFeatures(post, now))
                    .authorFeatures(buildAuthorFeatures(authorId, userMap, postCountMap, averagePopularityMap))
                    .interactionFeatures(buildInteractionFeatures(viewerId, authorId, now, viewerTotalInteractions))
                    .build();
        }).toList();
    }

    private InteractionFeatures buildInteractionFeatures(Long viewerId, Long authorId, LocalDateTime now, long viewerTotal) {
        long count7d = userInteractionRepository.countByViewerAndAuthorSince(viewerId, authorId, now.minusDays(7));
        long count30d = userInteractionRepository.countByViewerAndAuthorSince(viewerId, authorId, now.minusDays(30));

        LocalDateTime lastInteraction = userInteractionRepository.findLatestInteractionTime(viewerId, authorId);
        double hoursSinceLast = lastInteraction != null
                ? ChronoUnit.MINUTES.between(lastInteraction, now) / 60.0
                : 999.0;

        double affinity = viewerTotal > 0 ? (double) count30d / viewerTotal : 0.0;

        return InteractionFeatures.builder()
                .interactionCount7d(count7d)
                .interactionCount30d(count30d)
                .hoursSinceLastInteraction(hoursSinceLast)
                .affinityScore(affinity)
                .build();
    }

    private PostFeatures extractPostFeatures(Post post, LocalDateTime now) {
        long up = post.getUpvoteCount() != null ? post.getUpvoteCount() : 0L;
        long down = post.getDownvoteCount() != null ? post.getDownvoteCount() : 0L;
        double upvoteRatio = (up + down) > 0 ? (double) up / (up + down) : 0.5;
        double postAgeHours = post.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0
                : 0.0;
        double popularity = up + (post.getCmtCount() != null ? post.getCmtCount() : 0L)
                + (post.getShareCount() != null ? post.getShareCount() : 0L);

        long netScore = up - down;
        double hotScore = redditStyleHotScore(netScore, postAgeHours);

        return PostFeatures.builder()
                .postId(post.getId())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .hasMultimedia(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .hotScore(hotScore)
                .upvoteRatio(upvoteRatio)
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCmtCount())
                .viewCount(post.getViewCount())
                .shareCount(post.getShareCount())
                .popularity(popularity)
                .build();
    }

    private AuthorFeatures buildAuthorFeatures(
            Long authorId,
            Map<Long, User> userMap,
            Map<Long, Long> postCountMap,
            Map<Long, Double> averagePopularityMap) {
        String cacheKey = "author:features:" + authorId;
        String cached = redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            try {
                return objectMapper.readValue(cached, AuthorFeatures.class);
            } catch (Exception e) {
                log.warn("Failed to deserialize cached author features for authorId={}", authorId, e);
            }
        }

        User user = userMap.get(authorId);
        long postCount = postCountMap.getOrDefault(authorId, 0L);

        if (user != null && user.getCreatedAt() != null) {
            double seniorityYears = Math.max(ChronoUnit.DAYS.between(user.getCreatedAt(), LocalDateTime.now()), 0L) / 365.0;
            AuthorFeatures features = AuthorFeatures.builder()
                    .authorId(authorId)
                    .seniorityYears(seniorityYears)
                    .postCount(postCount)
                    .averagePopularity(averagePopularityMap.getOrDefault(authorId, 0.0))
                    .build();
            cacheAuthorFeatures(cacheKey, authorId, features);
            return features;
        }

        AuthorFeatures features = AuthorFeatures.builder()
                .authorId(authorId)
                .seniorityYears(0.0)
                .postCount(postCount)
                .averagePopularity(averagePopularityMap.getOrDefault(authorId, 0.0))
                .build();
        cacheAuthorFeatures(cacheKey, authorId, features);
        return features;
    }

    private void cacheAuthorFeatures(String cacheKey, Long authorId, AuthorFeatures features) {
        try {
            String json = objectMapper.writeValueAsString(features);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
        } catch (Exception e) {
            log.warn("Failed to cache author features for authorId={}", authorId, e);
        }
    }

    /**
     * Computes a hot score aligned with the Reddit formula used during training:
     * sign(score) * log10(max(|score|, 1)) + age_component
     *
     * <p>The age component uses post_age_hours normalized to the same scale as
     * Reddit's seconds/45000 divisor (≈ 12.5 hours per unit).</p>
     */
    private double redditStyleHotScore(long netScore, double postAgeHours) {
        double order = Math.log10(Math.max(Math.abs(netScore), 1));
        double sign = netScore > 0 ? 1.0 : netScore < 0 ? -1.0 : 0.0;
        double ageComponent = postAgeHours / 12.5;
        return sign * order + ageComponent;
    }
}
