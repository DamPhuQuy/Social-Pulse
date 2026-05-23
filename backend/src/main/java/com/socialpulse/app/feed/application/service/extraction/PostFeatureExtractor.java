package com.socialpulse.app.feed.application.service.extraction;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.temporal.ChronoUnit;

import com.socialpulse.app.feed.application.dto.features.support.PostFeatures;
import com.socialpulse.app.post.domain.model.Post;

public class PostFeatureExtractor {
    private static final double HOT_SCORE_TIME_DIVISOR = 45000.0;
    private static final long REDDIT_EPOCH = 1134028003L;

    public PostFeatures extract(Post post, LocalDateTime now) {
        long up = safe(post.getUpvoteCount());
        long down = safe(post.getDownvoteCount());
        double upvoteRatio = (up + down) > 0 ? (double) up / (up + down) : 0.5;
        double postAgeHours = post.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0 : 0.0;

        return PostFeatures.builder()
                .postId(post.getId())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .hasMultimedia(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .hotScore(hotScore(up - down, post.getCreatedAt()))
                .upvoteRatio(upvoteRatio)
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCmtCount())
                .viewCount(post.getViewCount())
                .shareCount(post.getShareCount())
                .build();
    }

    // Mirrors ai_pipeline.training.scanner._reddit_hot_score:
    // sign(score) * log10(max(|score|, 1)) + (created_epoch_seconds - REDDIT_EPOCH) / 45000.0
    // Newer posts get a larger recency term; age is already provided separately via post_age_hours.
    private double hotScore(long netScore, LocalDateTime createdAt) {
        double order = Math.log10(Math.max(Math.abs(netScore), 1));
        double sign = netScore > 0 ? 1.0 : netScore < 0 ? -1.0 : 0.0;
        if (createdAt == null) {
            return sign * order;
        }
        long createdEpochSeconds = createdAt.toEpochSecond(ZoneOffset.UTC);
        return sign * order + (createdEpochSeconds - REDDIT_EPOCH) / HOT_SCORE_TIME_DIVISOR;
    }

    private long safe(Long v) { return v != null ? v : 0L; }
}
