package com.socialpulse.app.feed.application.service;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.client.RestClient;

import com.socialpulse.app.behavior.application.dto.UserInteractionFeatures;
import com.socialpulse.app.behavior.application.usecase.ExtractBehaviorFeaturesUseCase;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.dto.ai.AiPostFeatures;
import com.socialpulse.app.feed.application.dto.ai.AiRankingRequest;
import com.socialpulse.app.feed.application.dto.ai.AiRankingResponse;
import com.socialpulse.app.feed.application.dto.ai.AiRelationshipFeatures;
import com.socialpulse.app.feed.application.dto.ai.AiUserFeatures;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;

public class AiRankingService implements PredictRankingUseCase {
    private final RestClient restClient;
    private final ExtractBehaviorFeaturesUseCase extractBehaviorFeaturesUseCase;

    public AiRankingService(
            @Value("${ai.service.url:http://localhost:8001}") String aiServiceUrl) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
        this.extractBehaviorFeaturesUseCase = extractBehaviorFeaturesUseCase;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        try {
            // Convert to AI service format
            AiRankingRequest aiRequest = convertToAiRequest(request);

            // Call AI service
            AiRankingResponse aiResponse = restClient.post()
                    .uri("/api/v1/rank/predict")
                    .body(aiRequest)
                    .retrieve()
                    .body(AiRankingResponse.class);

            if (aiResponse == null || aiResponse.getRankedPosts() == null) {
                return List.of();
            }

            // Convert response
            return aiResponse.getRankedPosts().stream()
                    .map(rankedPost -> RankingResponse.builder()
                            .postId(rankedPost.getPostId())
                            .score(rankedPost.getRankingScore())
                            .build())
                    .toList();

        } catch (Exception e) {
            throw new RuntimeException("Failed to get ranking scores from AI service", e);
        }
    }

    private AiRankingRequest convertToAiRequest(RankingRequest request) {
        if (request.getFeatures().isEmpty()) {
            throw new IllegalArgumentException("No features provided");
        }

        // Get user features from first item (same for all)
        RankingFeatures firstFeature = request.getFeatures().get(0);
        Long userId = firstFeature.getViewerFeatures().getUserId();

        // Convert user features
        AiUserFeatures userFeatures = AiUserFeatures.builder()
                .userId(userId)
                .followerCount(firstFeature.getViewerFeatures().getFollowerCount())
                .followingCount(firstFeature.getViewerFeatures().getFollowingCount())
                .postCount(firstFeature.getViewerFeatures().getPostCount())
                .accountAgeDays(365) // Placeholder - should come from user service
                .engagementRate(firstFeature.getViewerFeatures().getEngagementRate())
                .avgSessionDurationMinutes(15.0) // Placeholder
                .build();

        // Convert post features
        List<AiPostFeatures> candidatePosts = request.getFeatures().stream()
                .map(feature -> AiPostFeatures.builder()
                        .postId(feature.getPostId())
                        .authorId(feature.getAuthorFeatures().getUserId())
                        .topic("general") // Placeholder - should come from post
                        .createdAt(java.time.Instant.now().toString()) // Placeholder
                        .contentLength(500) // Placeholder
                        .hasImage(true) // Placeholder
                        .hasVideo(false) // Placeholder
                        .authorFollowerCount(feature.getAuthorFeatures().getFollowerCount())
                        .authorAvgEngagementRate(feature.getAuthorFeatures().getEngagementRate())
                        .predictedQualityScore(0.5) // Placeholder
                        .build())
                .toList();

        // Extract behavior features for all authors
        List<Long> authorIds = request.getFeatures().stream()
                .map(feature -> feature.getAuthorFeatures().getUserId())
                .distinct()
                .toList();

        List<UserInteractionFeatures> behaviorFeatures = extractBehaviorFeaturesUseCase.extractFeatures(userId, authorIds);

        // Create a map for quick lookup
        Map<Long, UserInteractionFeatures> behaviorFeaturesMap = behaviorFeatures.stream()
                .collect(Collectors.toMap(UserInteractionFeatures::getAuthorId, f -> f));

        // Convert relationship features with behavior data
        List<AiRelationshipFeatures> relationships = request.getFeatures().stream()
                .map(feature -> {
                    Long authorId = feature.getAuthorFeatures().getUserId();
                    UserInteractionFeatures behaviorFeature = behaviorFeaturesMap.get(authorId);

                    if (behaviorFeature != null) {
                        return AiRelationshipFeatures.builder()
                                .userId(userId)
                                .authorId(authorId)
                                .follows(behaviorFeature.isFollows())
                                .interactionCount7d(behaviorFeature.getInteractionCount7d())
                                .interactionCount30d(behaviorFeature.getInteractionCount30d())
                                .hoursSinceLastInteraction(behaviorFeature.getHoursSinceLastInteraction())
                                .affinityScore(behaviorFeature.getAffinityScore())
                                .build();
                    } else {
                        return AiRelationshipFeatures.builder()
                                .userId(userId)
                                .authorId(authorId)
                                .follows(false)
                                .interactionCount7d(0)
                                .interactionCount30d(0)
                                .hoursSinceLastInteraction(999.0)
                                .affinityScore(0.0)
                                .build();
                    }
                })
                .toList();

        return AiRankingRequest.builder()
                .userId(userId)
                .userFeatures(userFeatures)
                .candidatePosts(candidatePosts)
                .relationships(relationships)
                .build();
    }
}
