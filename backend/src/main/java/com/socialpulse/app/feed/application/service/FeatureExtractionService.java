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
import com.socialpulse.app.feed.application.dto.AuthorFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Extracts Pushshift-aligned ranking features from static post and author data.
 *
 * <p>Behavior and viewer-personalization signals are intentionally excluded from this contract.</p>
 */
@Slf4j
public class FeatureExtractionService implements ExtractFeaturesUseCase {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;
    private final UserRepository userRepository;
    private final PostRepository postRepository;

    public FeatureExtractionService(
            StringRedisTemplate redisTemplate,
            ObjectMapper objectMapper,
            UserRepository userRepository,
            PostRepository postRepository) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
        this.userRepository = userRepository;
        this.postRepository = postRepository;
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

        log.debug("Feature extraction batch queries: {} authors, {} author-post-counts, {} author-popularity-aggregates",
                userMap.size(), postCountMap.size(), averagePopularityMap.size());

        return candidates.stream().map(candidate -> {
            Post post = candidate.getPost();
            Long authorId = post.getUserId();

            return RankingFeatures.builder()
                    .postId(post.getId())
                    .postFeatures(extractPostFeatures(post, now))
                    .authorFeatures(buildAuthorFeatures(authorId, userMap, postCountMap, averagePopularityMap))
                    .build();
        }).toList();
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

        return PostFeatures.builder()
                .postId(post.getId())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .hasMultimedia(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .hotScore(post.getHotScore())
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
}
