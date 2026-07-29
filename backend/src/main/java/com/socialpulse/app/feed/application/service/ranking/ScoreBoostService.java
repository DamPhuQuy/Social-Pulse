package com.socialpulse.app.feed.application.service.ranking;

import java.time.Duration;
import java.time.LocalDateTime;

import org.springframework.stereotype.Component;

import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.post.domain.model.Post;

/**
 * Applies cold-start boosts so new posts aren't buried by seed data.
 * <ul>
 *   <li>Creator boost: own posts created within 60 min get +10 000</li>
 *   <li>Following boost: followed-author posts within 24 h get up to +5 000 (decays hourly)</li>
 * </ul>
 */
@Component
public class ScoreBoostService {

    public double boost(double baseScore, Long viewerId, CandidatePost candidate) {
        Post post = candidate.getPost();
        if (post.getCreatedAt() == null) return baseScore;

        LocalDateTime now = LocalDateTime.now();

        if (post.getUserId().equals(viewerId)) {
            long ageMinutes = Duration.between(post.getCreatedAt(), now).toMinutes();
            if (ageMinutes <= 60) return baseScore + 10_000.0;
        } else if (candidate.getSource() == Source.FOLLOWING) {
            long ageHours = Duration.between(post.getCreatedAt(), now).toHours();
            if (ageHours <= 24) return baseScore + 5_000.0 - (ageHours * 50.0);
        }

        return baseScore;
    }
}
