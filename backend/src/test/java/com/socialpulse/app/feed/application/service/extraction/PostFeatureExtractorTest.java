package com.socialpulse.app.feed.application.service.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;

import org.junit.jupiter.api.Test;

import com.socialpulse.app.post.domain.model.Post;

class PostFeatureExtractorTest {
    @Test
    void extractsLeakageSafePostFeatures() {
        PostFeatureExtractor extractor = new PostFeatureExtractor();
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 20, 6, 0);

        Post post = Post.builder()
                .id(1L)
                .content("hello world")
                .imageUrl("https://cdn.example.com/post.jpg")
                .upvoteCount(20L)
                .downvoteCount(3L)
                .cmtCount(9L)
                .shareCount(2L)
                .viewCount(100L)
                .hotScore(42.0)
                .createdAt(createdAt)
                .build();

        var features = extractor.extract(post, now);

        assertEquals(1L, features.getPostId());
        assertEquals(11, features.getContentLength());
        assertTrue(features.getHasMultimedia());
        assertFalse(features.getIsSharePost());
        assertEquals(4.0, features.getPostAgeHours(), 1e-9);
    }

    @Test
    void clampsMissingCreatedAtToZeroAge() {
        PostFeatureExtractor extractor = new PostFeatureExtractor();
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

        Post post = Post.builder()
                .id(1L)
                .content(null)
                .imageUrl("")
                .build();

        var features = extractor.extract(post, now);

        assertEquals(0, features.getContentLength());
        assertFalse(features.getHasMultimedia());
        assertEquals(0.0, features.getPostAgeHours(), 1e-9);
    }
}
