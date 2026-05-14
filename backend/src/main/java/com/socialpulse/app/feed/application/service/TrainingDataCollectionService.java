package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.feed.application.dto.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.TrainingDataStats;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeatureSnapshot;
import com.socialpulse.app.feed.domain.model.FeedImpression;
import com.socialpulse.app.feed.domain.model.TrainingDataRecord;
import com.socialpulse.app.feed.domain.repository.FeedImpressionRepository;
import com.socialpulse.app.feed.domain.repository.TrainingDataRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;

import lombok.extern.slf4j.Slf4j;

/**
 * Service for collecting training data for AI feed ranking model
 *
 * This service tracks:
 * 1. When users see posts (impressions)
 * 2. When users interact with posts (clicks, upvotes, comments, shares)
 * 3. Extracts features at impression time
 * 4. Labels samples based on user interactions
 */
@Slf4j
@Service
public class TrainingDataCollectionService {

    private final FeedImpressionRepository impressionRepository;
    private final TrainingDataRepository trainingDataRepository;
    private final PostRepository postRepository;
    private final FeatureExtractionService featureExtractionService;

    // Engagement events that count as positive labels
    private static final List<EventType> POSITIVE_EVENTS = List.of(
        EventType.UPVOTE,
        EventType.COMMENT,
        EventType.SHARE,
        EventType.CLICK  // Optional: can be considered positive
    );

    public TrainingDataCollectionService(
        FeedImpressionRepository impressionRepository,
        TrainingDataRepository trainingDataRepository,
        PostRepository postRepository,
        FeatureExtractionService featureExtractionService
    ) {
        this.impressionRepository = impressionRepository;
        this.trainingDataRepository = trainingDataRepository;
        this.postRepository = postRepository;
        this.featureExtractionService = featureExtractionService;
    }

    /**
     * Record when a user sees a post in their feed
     */
    @Async
    @Transactional
    public void recordImpression(
        Long userId,
        Long postId,
        int position,
        String rankingStrategy
    ) {
        try {
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                log.warn("Post {} not found for impression recording", postId);
                return;
            }

            Post post = postOpt.get();
            Long authorId = post.getUserId();

            // Create impression record
            FeedImpression impression = FeedImpression.builder()
                .userId(userId)
                .postId(postId)
                .authorId(authorId)
                .positionInFeed(position)
                .rankingStrategy(rankingStrategy)
                .impressionTime(LocalDateTime.now())
                .interacted(false)
                .build();

            impressionRepository.save(impression);

            log.debug("Recorded impression: user={}, post={}, position={}",
                userId, postId, position);

        } catch (Exception e) {
            log.error("Error recording impression for user={}, post={}",
                userId, postId, e);
        }
    }

    /**
     * Record when a user interacts with a post
     */
    @Async
    @Transactional
    public void recordInteraction(
        Long userId,
        Long postId,
        EventType eventType
    ) {
        try {
            // Find the most recent impression for this user-post pair
            Optional<FeedImpression> impressionOpt = impressionRepository
                .findMostRecentImpression(userId, postId);

            if (impressionOpt.isEmpty()) {
                log.warn("No impression found for interaction: user={}, post={}",
                    userId, postId);
                // Still create training sample even without impression record
                createTrainingDataFromInteraction(userId, postId, eventType);
                return;
            }

            FeedImpression impression = impressionOpt.get();

            // Update impression record
            impression.setInteracted(true);
            impression.setInteractionTime(LocalDateTime.now());
            impression.setInteractionType(eventType.name());
            impressionRepository.save(impression);

            // Create training data sample
            createTrainingDataFromImpression(impression, eventType);

            log.debug("Recorded interaction: user={}, post={}, type={}",
                userId, postId, eventType);

        } catch (Exception e) {
            log.error("Error recording interaction for user={}, post={}, type={}",
                userId, postId, eventType, e);
        }
    }

    private void createTrainingDataFromImpression(
        FeedImpression impression,
        EventType eventType
    ) {
        try {
            Long userId = impression.getUserId();
            Long postId = impression.getPostId();
            Long authorId = impression.getAuthorId();

            boolean isClicked = POSITIVE_EVENTS.contains(eventType);
            int relevance = isClicked ? 1 : 0;

            FeatureSnapshot features = extractFeaturesForTraining(
                userId, postId, authorId, impression.getImpressionTime(), isClicked
            );

            TrainingDataRecord record = TrainingDataRecord.builder()
                .userId(userId)
                .postId(postId)
                .authorId(authorId)
                .features(features)
                .relevance(relevance)
                .impressionTime(impression.getImpressionTime())
                .interactionTime(impression.getInteractionTime())
                .interactionType(eventType.name())
                .positionInFeed(impression.getPositionInFeed())
                .build();

            trainingDataRepository.save(record);

            log.debug("Created training sample: user={}, post={}, relevance={}",
                userId, postId, relevance);

        } catch (Exception e) {
            log.error("Error creating training data from impression: {}",
                impression.getId(), e);
        }
    }

    private void createTrainingDataFromInteraction(
        Long userId,
        Long postId,
        EventType eventType
    ) {
        try {
            Optional<Post> postOpt = postRepository.findById(postId);
            if (postOpt.isEmpty()) {
                return;
            }

            Post post = postOpt.get();
            Long authorId = post.getUserId();

            boolean isClicked = POSITIVE_EVENTS.contains(eventType);
            int relevance = isClicked ? 1 : 0;

            FeatureSnapshot features = extractFeaturesForTraining(
                userId, postId, authorId, LocalDateTime.now(), isClicked
            );

            TrainingDataRecord record = TrainingDataRecord.builder()
                .userId(userId)
                .postId(postId)
                .authorId(authorId)
                .features(features)
                .relevance(relevance)
                .impressionTime(LocalDateTime.now())
                .interactionTime(LocalDateTime.now())
                .interactionType(eventType.name())
                .positionInFeed(-1)  // Unknown position
                .build();

            trainingDataRepository.save(record);

        } catch (Exception e) {
            log.error("Error creating training data from interaction", e);
        }
    }

    private FeatureSnapshot extractFeaturesForTraining(
        Long userId,
        Long postId,
        Long authorId,
        LocalDateTime snapshotTime,
        Boolean isClicked
    ) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }
        
        CandidatePost cp = CandidatePost.builder()
            .post(postOpt.get())
            .source(Source.RANDOM)
            .build();
            
        List<RankingFeatures> featsList = featureExtractionService.extractFeatures(userId, List.of(cp));
        if (featsList.isEmpty()) {
            throw new RuntimeException("Could not extract features");
        }
        
        RankingFeatures rf = featsList.get(0);
        return convertToSnapshot(rf, isClicked, snapshotTime);
    }
    
    private FeatureSnapshot convertToSnapshot(RankingFeatures rf, Boolean clicked, LocalDateTime snapshotTime) {
        PostFeatures pf = rf.getPostFeatures();
        InteractionFeatures intF = rf.getInteractionFeatures();
        
        return FeatureSnapshot.builder()
            .viewerId(rf.getViewerFeatures().getUserId())
            .postId(pf.getPostId())
            .authorId(rf.getAuthorFeatures().getUserId())
            .hotScore(pf.getHotScore())
            .upvoteRatio(pf.getUpvoteRatio())
            .hasImage(pf.getHasImage())
            .contentLength(pf.getContentLength())
            .postAgeHours(pf.getPostAgeHours())
            .upvoteCount(safeCount(pf.getUpvoteCount()))
            .downvoteCount(safeCount(pf.getDownvoteCount()))
            .cmtCount(safeCount(pf.getCmtCount()))
            .shareCount(safeCount(pf.getShareCount()))
            .viewCount(safeCount(pf.getViewCount()))
            .interactionCount7d(intF != null ? intF.getInteractionCount7d() : 0)
            .interactionCount30d(intF != null ? intF.getInteractionCount30d() : 0)
            .affinityScore(intF != null ? intF.getAffinityScore() : 0.0)
            .lastInteractionHours(intF != null ? intF.getLastInteractionHours() : 999.0)
            .clicked(clicked)
            .snapshotTime(snapshotTime)
            .build();
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    @Transactional
    public void generateNegativeSamples(int hoursAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursAgo);

        List<FeedImpression> negativeImpressions = impressionRepository
            .findNonInteractedImpressionsSince(cutoffTime);

        log.info("Generating {} negative training samples", negativeImpressions.size());

        for (FeedImpression impression : negativeImpressions) {
            try {
                FeatureSnapshot features = extractFeaturesForTraining(
                    impression.getUserId(),
                    impression.getPostId(),
                    impression.getAuthorId(),
                    impression.getImpressionTime(),
                    false
                );

                TrainingDataRecord record = TrainingDataRecord.builder()
                    .userId(impression.getUserId())
                    .postId(impression.getPostId())
                    .authorId(impression.getAuthorId())
                    .features(features)
                    .relevance(0)  // Negative sample
                    .impressionTime(impression.getImpressionTime())
                    .interactionTime(null)
                    .interactionType(null)
                    .positionInFeed(impression.getPositionInFeed())
                    .build();

                trainingDataRepository.save(record);

            } catch (Exception e) {
                log.error("Error generating negative sample for impression: {}",
                    impression.getId(), e);
            }
        }

        log.info("Completed generating negative samples");
    }

    @Transactional(readOnly = true)
    public List<TrainingDataRecord> exportTrainingData(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int minInteractionsPerUser
    ) {
        log.info("Exporting training data from {} to {}", startDate, endDate);

        List<TrainingDataRecord> records = trainingDataRepository
            .findByImpressionTimeBetween(startDate, endDate);

        Map<Long, Long> userInteractionCounts = records.stream()
            .collect(Collectors.groupingBy(
                TrainingDataRecord::getUserId,
                Collectors.counting()
            ));

        List<TrainingDataRecord> filteredRecords = records.stream()
            .filter(r -> userInteractionCounts.get(r.getUserId()) >= minInteractionsPerUser)
            .collect(Collectors.toList());

        log.info("Exported {} training samples ({} users)",
            filteredRecords.size(), userInteractionCounts.size());

        return filteredRecords;
    }

    @Transactional(readOnly = true)
    public TrainingDataStats getTrainingDataStats() {
        long totalSamples = trainingDataRepository.count();
        long positiveSamples = trainingDataRepository.countByRelevance(1);
        long negativeSamples = trainingDataRepository.countByRelevance(0);
        long uniqueUsers = trainingDataRepository.countDistinctUsers();
        long uniquePosts = trainingDataRepository.countDistinctPosts();

        double positiveRate = totalSamples > 0
            ? (double) positiveSamples / totalSamples
            : 0.0;

        return TrainingDataStats.builder()
            .totalSamples(totalSamples)
            .positiveSamples(positiveSamples)
            .negativeSamples(negativeSamples)
            .positiveRate(positiveRate)
            .uniqueUsers(uniqueUsers)
            .uniquePosts(uniquePosts)
            .build();
    }
}
