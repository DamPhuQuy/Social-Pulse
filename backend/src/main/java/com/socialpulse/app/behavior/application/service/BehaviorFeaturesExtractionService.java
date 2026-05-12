package com.socialpulse.app.behavior.application.service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.socialpulse.app.behavior.application.dto.UserInteractionFeatures;
import com.socialpulse.app.behavior.application.usecase.BehaviorFeaturesExtractionUseCase;
import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class BehaviorFeaturesExtractionService implements BehaviorFeaturesExtractionUseCase {
    private final UserBehaviorRepository behaviorRepository;
    private final FollowRepository followRepository;
    private final PostRepository postRepository;

    public BehaviorFeaturesExtractionService(
            UserBehaviorRepository behaviorRepository,
            FollowRepository followRepository,
            PostRepository postRepository) {
        this.behaviorRepository = behaviorRepository;
        this.followRepository = followRepository;
        this.postRepository = postRepository;
    }

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

        // Group behaviors by AUTHOR (fixed: was grouping by postId)
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
                .map(time -> (double) Duration.between(time, now).toHours())
                .orElse(999.0);

        // Calculate affinity score (weighted by recency and engagement type)
        double affinityScore = calculateAffinityScore(behaviors30d, now);

        return UserInteractionFeatures.builder()
                .userId(userId)
                .authorId(authorId)
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

        // Updated weights: SHARE=8, HIDE=-5, REPORT=-8
        Map<EventType, Double> eventWeights = Map.of(
                EventType.CLICK, 1.0,
                EventType.UPVOTE, 3.0,
                EventType.COMMENT, 5.0,
                EventType.SHARE, 8.0,
                EventType.FOLLOW, 10.0,
                EventType.DOWNVOTE, -2.0,
                EventType.HIDE, -5.0,
                EventType.REPORT, -8.0
        );

        double totalScore = 0.0;
        for (UserBehavior behavior : behaviors) {
            double weight = eventWeights.getOrDefault(behavior.getEventType(), 0.0);

            // Apply time decay (exponential decay with τ = 30 days = 720 hours)
            long hoursAgo = Duration.between(behavior.getEventTime(), now).toHours();
            double timeDecay = Math.exp(-hoursAgo / 720.0);

            totalScore += weight * timeDecay;
        }

        return totalScore;
    }

    /**
     * Groups behaviors by author ID by looking up the post's author.
     * Uses batch query to fetch posts for all postIds, then maps postId → authorId.
     */
    private Map<Long, List<UserBehavior>> groupBehaviorsByAuthor(List<UserBehavior> behaviors) {
        if (behaviors.isEmpty()) {
            return Map.of();
        }

        // Collect unique postIds from behaviors
        Set<Long> postIds = behaviors.stream()
                .map(UserBehavior::getPostId)
                .filter(id -> id != null)
                .collect(Collectors.toSet());

        // Build postId → authorId mapping via batch lookup
        Map<Long, Long> postToAuthorMap = new HashMap<>();
        for (Long postId : postIds) {
            postRepository.findById(postId).ifPresent(post ->
                    postToAuthorMap.put(postId, post.getUserId()));
        }

        // Group behaviors by authorId
        return behaviors.stream()
                .filter(b -> b.getPostId() != null && postToAuthorMap.containsKey(b.getPostId()))
                .collect(Collectors.groupingBy(b -> postToAuthorMap.get(b.getPostId())));
    }
}
