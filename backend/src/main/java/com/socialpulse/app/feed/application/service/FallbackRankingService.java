package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;

@Service
public class FallbackRankingService {
    public List<RankingResponse> rank(List<CandidatePost> candidates) {
        LocalDateTime now = LocalDateTime.now();

        return candidates.stream()
                .map(candidate -> RankingResponse.builder()
                        .postId(candidate.getPost().getId())
                        .score(calculateScore(candidate.getPost(), now))
                        .featureSchemaVersion("v1")
                        .build())
                .toList();
    }

    private double calculateScore(Post post, LocalDateTime now) {
        double hotScore = post.getHotScore() != null ? post.getHotScore() : 0.0;
        double commentScore = Math.log1p(safeCount(post.getCmtCount())) * 0.8;
        double shareScore = Math.log1p(safeCount(post.getShareCount())) * 1.2;
        double viewScore = Math.log1p(safeCount(post.getViewCount())) * 0.15;
        double freshnessPenalty = ageHours(post, now) * 0.12;
        double imageBoost = post.getImageUrl() != null && !post.getImageUrl().isBlank() ? 0.25 : 0.0;

        return hotScore + commentScore + shareScore + viewScore + imageBoost - freshnessPenalty;
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
