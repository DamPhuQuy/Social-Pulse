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
    private final ContentAnalysisService contentAnalysisService;
    private final UserInterestProfileService userInterestProfileService;

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
        FeatureExtractionService featureExtractionService,
        ContentAnalysisService contentAnalysisService,
        UserInterestProfileService userInterestProfileService
    ) {
        this.impressionRepository = impressionRepository;
        this.trainingDataRepository = trainingDataRepository;
        this.postRepository = postRepository;
        this.featureExtractionService = featureExtractionService;
        this.contentAnalysisService = contentAnalysisService;
        this.userInterestProfileService = userInterestProfileService;
    }

    /**
     * Record when a user sees a post in their feed
     * This is called when the feed is generated and shown to the user
     *
     * @param userId User who saw the post
     * @param postId Post that was shown
     * @param position Position in the feed (0-indexed)
     * @param rankingStrategy Strategy used to rank the feed
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
     * This updates the impression record and creates a training sample
     *
     * @param userId User who interacted
     * @param postId Post that was interacted with
     * @param eventType Type of interaction
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

    /**
     * Create training data sample from impression and interaction
     */
    private void createTrainingDataFromImpression(
        FeedImpression impression,
        EventType eventType
    ) {
        try {
            Long userId = impression.getUserId();
            Long postId = impression.getPostId();
            Long authorId = impression.getAuthorId();

            // Extract features at impression time
            CompleteRankingFeatures features = extractFeaturesForTraining(
                userId, postId, authorId, impression.getImpressionTime()
            );

            // Determine label (relevance)
            int relevance = POSITIVE_EVENTS.contains(eventType) ? 1 : 0;

            // Create training record
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

    /**
     * Create training data sample from interaction (when no impression exists)
     */
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

            // Extract features at current time
            CompleteRankingFeatures features = extractFeaturesForTraining(
                userId, postId, authorId, LocalDateTime.now()
            );

            int relevance = POSITIVE_EVENTS.contains(eventType) ? 1 : 0;

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

    /**
     * Extract all features for a user-post pair at a specific time
     */
    private CompleteRankingFeatures extractFeaturesForTraining(
        Long userId,
        Long postId,
        Long authorId,
        LocalDateTime impressionTime
    ) {
        Optional<Post> postOpt = postRepository.findById(postId);
        if (postOpt.isEmpty()) {
            throw new IllegalArgumentException("Post not found: " + postId);
        }

        Post post = postOpt.get();

        // Extract content features
        ContentFeatures contentFeatures = extractContentFeatures(userId, post);

        // Extract author features
        AuthorFeatures authorFeatures = extractAuthorFeatures(authorId);

        // Extract relationship features
        RelationshipFeatures relationshipFeatures = extractRelationshipFeatures(
            userId, authorId, impressionTime
        );

        // Extract engagement features (at impression time)
        EngagementFeatures engagementFeatures = extractEngagementFeatures(post);

        return CompleteRankingFeatures.builder()
            .postId(postId)
            .contentFeatures(contentFeatures)
            .authorFeatures(authorFeatures)
            .relationshipFeatures(relationshipFeatures)
            .engagementFeatures(engagementFeatures)
            .build();
    }

    private ContentFeatures extractContentFeatures(Long userId, Post post) {
        String content = post.getContent();

        // Extract keywords and hashtags
        List<String> keywords = contentAnalysisService.extractKeywords(content);
        List<String> hashtags = contentAnalysisService.extractHashtags(content);

        // Get user interest profiles
        Map<String, Double> keywordProfile = userInterestProfileService.buildKeywordProfile(userId);
        Map<String, Double> hashtagProfile = userInterestProfileService.buildHashtagProfile(userId);

        // Calculate relevance scores
        double keywordsRelevance = userInterestProfileService.calculateKeywordRelevance(
            keywords, keywordProfile
        );
        double hashtagsRelevance = userInterestProfileService.calculateHashtagRelevance(
            hashtags, hashtagProfile
        );

        return ContentFeatures.builder()
            .keywordsRelevance(keywordsRelevance)
            .hashtagsRelevance(hashtagsRelevance)
            .mentionsRelevance(0)  // TODO: implement mention detection
            .contentLength(contentAnalysisService.getContentLength(content))
            .hasHashtags(contentAnalysisService.containsHashtags(content) ? 1 : 0)
            .hasUrl(contentAnalysisService.containsUrl(content) ? 1 : 0)
            .hasMultimedia(post.getImageUrl() != null ? 1 : 0)
            .build();
    }

    private AuthorFeatures extractAuthorFeatures(Long authorId) {
        // TODO: Query actual user stats from database
        return AuthorFeatures.builder()
            .authorId(authorId)
            .followerCount(0)
            .followingCount(0)
            .followersFollowingsRatio(0.0)
            .seniority(0.0)
            .postCount(0)
            .engagementRate(0.0)
            .build();
    }

    private RelationshipFeatures extractRelationshipFeatures(
        Long userId,
        Long authorId,
        LocalDateTime impressionTime
    ) {
        // TODO: Implement actual relationship feature extraction
        return RelationshipFeatures.builder()
            .follows(0)
            .interactionCount7d(0)
            .interactionCount30d(0)
            .hoursSinceLastInteraction(999.0)
            .affinityScore(0.0)
            .build();
    }

    private EngagementFeatures extractEngagementFeatures(Post post) {
        long popularity = safeCount(post.getUpvoteCount())
            + safeCount(post.getDownvoteCount())
            + safeCount(post.getCmtCount())
            + safeCount(post.getShareCount())
            + safeCount(post.getViewCount());

        return EngagementFeatures.builder()
            .popularity(popularity)
            .upvoteCount(safeCount(post.getUpvoteCount()))
            .downvoteCount(safeCount(post.getDownvoteCount()))
            .commentCount(safeCount(post.getCmtCount()))
            .shareCount(safeCount(post.getShareCount()))
            .viewCount(safeCount(post.getViewCount()))
            .build();
    }

    private long safeCount(Long value) {
        return value == null ? 0L : value;
    }

    /**
     * Generate negative samples (impressions without interactions)
     * This should be run periodically to create training samples for posts
     * that users saw but didn't interact with
     *
     * @param hoursAgo Look back this many hours for impressions
     */
    @Transactional
    public void generateNegativeSamples(int hoursAgo) {
        LocalDateTime cutoffTime = LocalDateTime.now().minusHours(hoursAgo);

        // Find impressions without interactions
        List<FeedImpression> negativeImpressions = impressionRepository
            .findNonInteractedImpressionsSince(cutoffTime);

        log.info("Generating {} negative training samples", negativeImpressions.size());

        for (FeedImpression impression : negativeImpressions) {
            try {
                CompleteRankingFeatures features = extractFeaturesForTraining(
                    impression.getUserId(),
                    impression.getPostId(),
                    impression.getAuthorId(),
                    impression.getImpressionTime()
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

    /**
     * Export training data to CSV for model training
     *
     * @param startDate Start date for export
     * @param endDate End date for export
     * @param minInteractionsPerUser Minimum interactions per user to include
     * @return List of training records
     */
    @Transactional(readOnly = true)
    public List<TrainingDataRecord> exportTrainingData(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int minInteractionsPerUser
    ) {
        log.info("Exporting training data from {} to {}", startDate, endDate);

        List<TrainingDataRecord> records = trainingDataRepository
            .findByImpressionTimeBetween(startDate, endDate);

        // Filter users with minimum interactions
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

    /**
     * Get training data statistics
     */
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
