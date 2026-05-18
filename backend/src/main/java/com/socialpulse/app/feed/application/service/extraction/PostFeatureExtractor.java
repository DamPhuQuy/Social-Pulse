package com.socialpulse.app.feed.application.service.extraction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.socialpulse.app.feed.application.dto.features.support.PostFeatures;
import com.socialpulse.app.post.domain.model.Post;

public class PostFeatureExtractor {

    public PostFeatures extract(Post post, LocalDateTime now) {
        long up = safe(post.getUpvoteCount());
        long down = safe(post.getDownvoteCount());
        double upvoteRatio = (up + down) > 0 ? (double) up / (up + down) : 0.5;
        double postAgeHours = post.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0 : 0.0;
        double popularity = up + safe(post.getCmtCount()) + safe(post.getShareCount());

        return PostFeatures.builder()
                .postId(post.getId())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .hasMultimedia(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .hotScore(hotScore(up - down, postAgeHours))
                .upvoteRatio(upvoteRatio)
                .upvoteCount(post.getUpvoteCount())
                .downvoteCount(post.getDownvoteCount())
                .commentCount(post.getCmtCount())
                .viewCount(post.getViewCount())
                .shareCount(post.getShareCount())
                .popularity(popularity)
                .build();
    }

    // sign(score) * log10(max(|score|, 1)) + age_hours/12.5  (mirrors Reddit training formula)
    private double hotScore(long netScore, double postAgeHours) {
        double order = Math.log10(Math.max(Math.abs(netScore), 1));
        double sign = netScore > 0 ? 1.0 : netScore < 0 ? -1.0 : 0.0;
        return sign * order + postAgeHours / 12.5;
    }

    private long safe(Long v) { return v != null ? v : 0L; }
}
