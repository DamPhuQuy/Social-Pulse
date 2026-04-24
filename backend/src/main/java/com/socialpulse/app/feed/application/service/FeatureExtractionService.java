package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.redis.core.StringRedisTemplate;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.UserFeatures;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;

public class FeatureExtractionService implements ExtractFeaturesUseCase {
    private final StringRedisTemplate redisTemplate;
    private final ObjectMapper objectMapper;

    public FeatureExtractionService(StringRedisTemplate redisTemplate, ObjectMapper objectMapper) {
        this.redisTemplate = redisTemplate;
        this.objectMapper = objectMapper;
    }

    @Override
    public List<RankingFeatures> extractFeatures(Long viewerId, List<CandidatePost> candidates) {
        UserFeatures viewerFeatures = getUserFeatures(viewerId);

        return candidates.stream()
                .map(candidate -> {
                    Post post = candidate.getPost();
                    PostFeatures postFeatures = extractPostFeatures(post);
                    UserFeatures authorFeatures = getUserFeatures(post.getUserId());

                    return RankingFeatures.builder()
                            .postId(post.getId())
                            .postFeatures(postFeatures)
                            .authorFeatures(authorFeatures)
                            .viewerFeatures(viewerFeatures)
                            .build();
                })
                .toList();
    }

    private PostFeatures extractPostFeatures(Post post) {
        double recencyScore = calculateRecencyScore(post.getCreatedAt());

        return PostFeatures.builder()
                .postId(post.getId())
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .cmtCount(post.getCmtCount())
                .viewCount(post.getViewCount())
                .shareCount(post.getShareCount())
                .hotScore(post.getHotScore())
                .recencyScore(recencyScore)
                .postType(post.getType().name())
                .build();
    }

    private UserFeatures getUserFeatures(Long userId) {
        String cacheKey = "user:features:" + userId;
        String cached = redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            try {
                return objectMapper.readValue(cached, UserFeatures.class);
            } catch (Exception e) {
                // Log error and continue to fetch fresh data
            }
        }

        UserFeatures features = UserFeatures.builder()
                .userId(userId)
                .followerCount(0)
                .followingCount(0)
                .engagementRate(0.0)
                .postCount(0)
                .build();

        try {
            String json = objectMapper.writeValueAsString(features);
            redisTemplate.opsForValue().set(cacheKey, json, Duration.ofMinutes(10));
        } catch (Exception e) {
            // Log error but don't fail
        }

        return features;
    }

    private double calculateRecencyScore(LocalDateTime createdAt) {
        long hoursAgo = Duration.between(createdAt, LocalDateTime.now()).toHours();
        return Math.max(0, 1.0 - (hoursAgo / 168.0));
    }
}
