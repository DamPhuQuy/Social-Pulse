package com.socialpulse.app.feed.application.service.ranking;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import com.socialpulse.app.feed.application.dto.response.RankingResponse;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;

@Component
public class RuleBasedRankingService {
    private static final double HALF_LIFE_HOURS = 24.0;
    private static final double MAX_ENGAGEMENT_CAP = 500.0;
    private final String featureSchemaVersion;

    public RuleBasedRankingService() {
        this("v2");
    }

    public RuleBasedRankingService(@Value("${feed.ranking.schema-version:v2}") String featureSchemaVersion) {
        this.featureSchemaVersion = featureSchemaVersion;
    }

    public List<RankingResponse> rank(List<CandidatePost> candidates) {
        LocalDateTime now = LocalDateTime.now();

        return candidates.stream()
                .map(candidate -> RankingResponse.builder()
                        .postId(candidate.getPost().getId())
                        .score(calculateScore(candidate.getPost(), now))
                        .featureSchemaVersion(featureSchemaVersion)
                        .build())
                .toList();
    }

    private double calculateScore(Post post, LocalDateTime now) {
        double ageInHours = ageHours(post, now);
        double recencyScore = Math.pow(0.5, ageInHours / HALF_LIFE_HOURS);

        long likes = safeCount(post.getUpvoteCount());
        long comments = safeCount(post.getCmtCount());
        long shares = safeCount(post.getShareCount());
        long views = safeCount(post.getViewCount());

        double rawEngagement = likes * 1.0 + comments * 2.0 + shares * 2.0 + (views * 0.1);
        double engagementScore = Math.min(1.0, Math.log1p(rawEngagement) / Math.log1p(MAX_ENGAGEMENT_CAP));

        double imageBoost = (post.getImageUrl() != null && !post.getImageUrl().isBlank()) ? 0.05 : 0.0;

        double totalScore = (recencyScore * 0.6) + (engagementScore * 0.4) + imageBoost;
        return Math.min(1.0, totalScore);
    }

    private double ageHours(Post post, LocalDateTime now) {
        if (post.getCreatedAt() == null) {
            return 0.0;
        }
        return Math.max(0.0, Duration.between(post.getCreatedAt(), now).toMinutes() / 60.0);
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }
}
