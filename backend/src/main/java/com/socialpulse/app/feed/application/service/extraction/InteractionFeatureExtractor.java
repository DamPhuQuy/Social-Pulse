package com.socialpulse.app.feed.application.service.extraction;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;

import com.socialpulse.app.feed.application.dto.features.support.InteractionFeatures;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;

public class InteractionFeatureExtractor {
    private final UserInteractionRepository userInteractionRepository;

    public InteractionFeatureExtractor(UserInteractionRepository userInteractionRepository) {
        this.userInteractionRepository = userInteractionRepository;
    }

    public InteractionFeatures extract(Long viewerId, Long authorId, LocalDateTime now, long viewerTotal) {
        long count7d = userInteractionRepository.countByViewerAndAuthorSince(viewerId, authorId, now.minusDays(7));
        long count30d = userInteractionRepository.countByViewerAndAuthorSince(viewerId, authorId, now.minusDays(30));

        LocalDateTime last = userInteractionRepository.findLatestInteractionTime(viewerId, authorId);
        double hoursSinceLast = last != null ? ChronoUnit.MINUTES.between(last, now) / 60.0 : 999.0;

        return InteractionFeatures.builder()
                .interactionCount7d(count7d)
                .interactionCount30d(count30d)
                .hoursSinceLastInteraction(hoursSinceLast)
                .affinityScore(viewerTotal > 0 ? (double) count30d / viewerTotal : 0.0)
                .build();
    }
}
