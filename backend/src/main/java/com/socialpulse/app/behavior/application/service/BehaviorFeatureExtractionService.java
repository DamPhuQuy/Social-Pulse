package com.socialpulse.app.behavior.application.service;

import com.socialpulse.app.behavior.application.dto.UserInteractionFeatures;
import com.socialpulse.app.behavior.application.usecase.ExtractBehaviorFeaturesUseCase;
import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class BehaviorFeatureExtractionService implements ExtractBehaviorFeaturesUseCase {
    private final UserBehaviorRepository behaviorRepository;

    private static final List<EventType> ENGAGEMENT_EVENTS = Arrays.asList(
            EventType.CLICK,
            EventType.UPVOTE,
            EventType.DOWNVOTE,
            EventType.COMMENT,
            EventType.SHARE,
            EventType.FOLLOW
    );

    @Override
    public List<UserInteractionFeatures> extractFeatures(Long userId, List<Long> authorIds) {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime sevenDaysAgo = now.minusDays(7);
        LocalDateTime thirtyDaysAgo = now.minusDays(30);

        // Get all user behaviors in the last 30 days
        List<UserBehavior> behaviors30d = behaviorRepository.findByUserIdSince(userId, thirtyDaysAgo);
        List<UserBehavior> behaviors7d = behaviors30d.stream()
                .filter(b -> b.getEventTime().isAfter(sevenDaysAgo))
                .collect(Collectors.toList());

        // Group behaviors by author (assuming we can get author from post)
        Map<Long, List<UserBehavior>> behaviorsByAuthor30d = groupBehaviorsByAuthor(behaviors30d);
        Map<Long, List<UserBehavior>> behaviorsByAuthor7d = groupBehaviorsByAuthor(behaviors7d);

        // Extract features for each author
        return authorIds.stream()
                .map(authorId -> extractFeaturesForAuthor(
                        userId,
                        authorId,
                        behaviorsByAuthor7d.getOrDefault(authorId, Collections.emptyList()),
                        behaviorsByAuthor30d.getOrDefault(authorId, Collections.emptyList()),
                        now
                ))
                .collect(Collectors.toList());
    }

    private UserInteractionFeatures extractFeaturesForAuthor(
            Long userId,
            Long authorId,
            List<UserBehavior> behaviors7d,
            List<UserBehavior> behaviors30d,
            LocalDateTime now
    ) {
        // Count engagement interactions
        int interactionCount7d = (int) behaviors7d.stream()
                .filter(b -> ENGAGEMENT_EVENTS.contains(b.getEventType()))
                .count();

        int interactionCount30d = (int) behaviors30d.stream()
                .filter(b -> ENGAGEMENT_EVENTS.contains(b.getEventType()))
                .count();

        // Find last interaction time
        Optional<LocalDateTime> lastInteractionTime = behaviors30d.stream()
                .filter(b -> ENGAGEMENT_EVENTS.contains(b.getEventType()))
                .map(UserBehavior::getEventTime)
                .max(LocalDateTime::compareTo);

        double hoursSinceLastInteraction = lastInteractionTime
                .map(time -> Duration.between(time, now).toHours())
                .orElse(999.0);

        // Calculate affinity score (weighted by recency and engagement type)
        double affinityScore = calculateAffinityScore(behaviors30d, now);

        // Check if user follows author (would need to query follow relationship)
        boolean follows = checkFollowRelationship(userId, authorId);

        return UserInteractionFeatures.builder()
                .userId(userId)
                .authorId(authorId)
                .follows(follows)
                .interactionCount7d(interactionCount7d)
                .interactionCount30d(interactionCount30d)
                .hoursSinceLastInteraction(hoursSinceLastInteraction)
                .affinityScore(affinityScore)
                .build();
    }

    private double calculateAffinityScore(List<UserBehavior> behaviors, LocalDateTime now) {
        if (behaviors.isEmpty()) {
            return 0.0;
        }

        // Weight different event types
        Map<EventType, Double> eventWeights = Map.of(
                EventType.CLICK, 1.0,
                EventType.UPVOTE, 3.0,
                EventType.COMMENT, 5.0,
                EventType.SHARE, 7.0,
                EventType.FOLLOW, 10.0,
                EventType.DOWNVOTE, -2.0
        );

        double totalScore = 0.0;
        for (UserBehavior behavior : behaviors) {
            double weight = eventWeights.getOrDefault(behavior.getEventType(), 0.0);

            // Apply time decay (exponential decay over 30 days)
            long hoursAgo = Duration.between(behavior.getEventTime(), now).toHours();
            double timeDecay = Math.exp(-hoursAgo / (30.0 * 24.0));

            totalScore += weight * timeDecay;
        }

        return totalScore;
    }

    private Map<Long, List<UserBehavior>> groupBehaviorsByAuthor(List<UserBehavior> behaviors) {
        // This is a placeholder - in reality, you'd need to join with posts to get author_id
        // For now, we'll use postId as a proxy (you'll need to enhance this)
        return behaviors.stream()
                .collect(Collectors.groupingBy(UserBehavior::getPostId));
    }

    private boolean checkFollowRelationship(Long userId, Long authorId) {
        // TODO: Query follow relationship from database
        // This would require access to a FollowRepository
        return false;
    }
}
