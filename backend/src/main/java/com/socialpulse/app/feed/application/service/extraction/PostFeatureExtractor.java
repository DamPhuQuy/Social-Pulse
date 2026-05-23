package com.socialpulse.app.feed.application.service.extraction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.socialpulse.app.feed.application.dto.features.support.PostFeatures;
import com.socialpulse.app.post.domain.model.Post;

public class PostFeatureExtractor {
    public PostFeatures extract(Post post, LocalDateTime now) {
        double postAgeHours = post.getCreatedAt() != null
                ? ChronoUnit.MINUTES.between(post.getCreatedAt(), now) / 60.0 : 0.0;

        return PostFeatures.builder()
                .postId(post.getId())
                .contentLength(post.getContent() != null ? post.getContent().length() : 0)
                .hasMultimedia(post.getImageUrl() != null && !post.getImageUrl().isBlank())
                .isSharePost(post.isSharedPost())
                .postAgeHours(postAgeHours)
                .build();
    }
}
