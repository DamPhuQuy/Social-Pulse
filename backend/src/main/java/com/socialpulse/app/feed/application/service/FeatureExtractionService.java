package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.UserFeatures;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;

@Service
public class FeatureExtractionService {
    private final RedisTemplate<String, Object> redisTemplate;

    public FeatureExtractionService(RedisTemplate<String, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

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
                .collect(Collectors.toList());
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
        UserFeatures cached = (UserFeatures) redisTemplate.opsForValue().get(cacheKey);

        if (cached != null) {
            return cached;
        }

        UserFeatures features = UserFeatures.builder()
                .userId(userId)
                .followerCount(0)
                .followingCount(0)
                .engagementRate(0.0)
                .postCount(0)
                .build();

        redisTemplate.opsForValue().set(cacheKey, features, Duration.ofMinutes(10));
        return features;
    }

    private double calculateRecencyScore(LocalDateTime createdAt) {
        long hoursAgo = Duration.between(createdAt, LocalDateTime.now()).toHours();
        return Math.max(0, 1.0 - (hoursAgo / 168.0));
    }
}
