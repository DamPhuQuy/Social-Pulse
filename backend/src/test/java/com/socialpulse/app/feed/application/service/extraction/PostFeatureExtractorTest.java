package com.socialpulse.app.feed.application.service.extraction;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalDateTime;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.socialpulse.app.post.domain.model.Post;

class PostFeatureExtractorTest {
    private static final double HOT_SCORE_TIME_DIVISOR = 45000.0;
    private static final long REDDIT_EPOCH = 1134028003L;

    @Test
    void computesHotScoreUsingRedditStyleRecencyTerm() {
        PostFeatureExtractor extractor = new PostFeatureExtractor();
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);
        LocalDateTime createdAt = LocalDateTime.of(2026, 5, 20, 6, 0);

        Post post = Post.builder()
                .id(1L)
                .content("hello world")
                .upvoteCount(20L)
                .downvoteCount(3L)
                .createdAt(createdAt)
                .build();

        double actual = extractor.extract(post, now).getHotScore();
        double expected = expectedHotScore(17L, createdAt);

        assertEquals(expected, actual, 1e-9);
    }

    @Test
    void assignsHigherHotScoreToNewerPostWhenNetScoreMatches() {
        PostFeatureExtractor extractor = new PostFeatureExtractor();
        LocalDateTime now = LocalDateTime.of(2026, 5, 20, 10, 0);

        Post newer = Post.builder()
                .id(1L)
                .content("newer")
                .upvoteCount(10L)
                .downvoteCount(2L)
                .createdAt(now.minusHours(2))
                .build();

        Post older = Post.builder()
                .id(2L)
                .content("older")
                .upvoteCount(10L)
                .downvoteCount(2L)
                .createdAt(now.minusHours(24))
                .build();

        double newerScore = extractor.extract(newer, now).getHotScore();
        double olderScore = extractor.extract(older, now).getHotScore();

        assertTrue(newerScore > olderScore);
    }

    private static double expectedHotScore(long netScore, LocalDateTime createdAt) {
        double order = Math.log10(Math.max(Math.abs(netScore), 1));
        double sign = netScore > 0 ? 1.0 : netScore < 0 ? -1.0 : 0.0;
        long createdEpochSeconds = createdAt.toEpochSecond(ZoneOffset.UTC);
        return sign * order + (createdEpochSeconds - REDDIT_EPOCH) / HOT_SCORE_TIME_DIVISOR;
    }
}
