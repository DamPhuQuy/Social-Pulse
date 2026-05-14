package com.socialpulse.app.feed.application.service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import com.socialpulse.app.behavior.application.usecase.BehaviorFeaturesExtractionUseCase;
import com.socialpulse.app.feed.application.dto.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.dto.UserFeatures;
import com.socialpulse.app.feed.application.dto.ai.AiRankingResponse;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class AiRankingService implements PredictRankingUseCase {
    private final RestClient restClient;
    private final BehaviorFeaturesExtractionUseCase extractBehaviorFeaturesUseCase;
    private final boolean aiServiceEnabled;

    public AiRankingService(
            @Value("${ai.service.url:http://localhost:5000}") String aiServiceUrl,
            @Value("${ai.service.enabled:false}") boolean aiServiceEnabled,
            BehaviorFeaturesExtractionUseCase extractBehaviorFeaturesUseCase) {
        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .build();
        this.extractBehaviorFeaturesUseCase = extractBehaviorFeaturesUseCase;
        this.aiServiceEnabled = aiServiceEnabled;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        if (!aiServiceEnabled) {
            log.debug("AI service disabled, returning empty scores");
            return List.of();
        }

        if (request.getFeatures().isEmpty()) {
            return List.of();
        }

        try {
            // Convert to simplified prediction API format
            Map<String, Object> predictionRequest = convertToPredictionRequest(request);

            // Call AI service
            AiRankingResponse aiResponse = restClient.post()
                    .uri("/predict")
                    .body(predictionRequest)
                    .retrieve()
                    .body(AiRankingResponse.class);

            if (aiResponse == null || aiResponse.getRankedPosts() == null) {
                log.warn("AI service returned null response");
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
            log.error("Failed to get ranking scores from AI service, returning empty", e);
            return List.of();
        }
    }

    /**
     * Convert RankingRequest to simplified prediction API format
     */
    private Map<String, Object> convertToPredictionRequest(RankingRequest request) {
        RankingFeatures firstFeature = request.getFeatures().get(0);
        Long userId = firstFeature.getViewerFeatures().getUserId();

        List<Map<String, Object>> posts = new ArrayList<>();

        for (RankingFeatures feature : request.getFeatures()) {
            Map<String, Object> postData = new HashMap<>();
            postData.put("post_id", feature.getPostId());
            postData.put("features", extractFeatureVector(feature));
            posts.add(postData);
        }

        Map<String, Object> predictionRequest = new HashMap<>();
        predictionRequest.put("user_id", userId);
        predictionRequest.put("posts", posts);

        return predictionRequest;
    }

    /**
     * Extract 26 features from RankingFeatures
     */
    private Map<String, Object> extractFeatureVector(RankingFeatures features) {
        Map<String, Object> featureVector = new HashMap<>();

        PostFeatures postFeats = features.getPostFeatures();
        UserFeatures authorFeats = features.getAuthorFeatures();
        InteractionFeatures interactionFeats = features.getInteractionFeatures();

        // Content features (7)
        // Note: Some features are placeholders until Phase 2 feature extraction is fully integrated
        featureVector.put("keywords_relevance", 0.0); // TODO: Implement in ContentAnalysisService
        featureVector.put("hashtags_relevance", 0.0); // TODO: Implement in ContentAnalysisService
        featureVector.put("mentions_relevance", 0.0); // TODO: Implement in ContentAnalysisService
        featureVector.put("content_length", postFeats.getContentLength() != null ? postFeats.getContentLength() : 0);
        featureVector.put("has_hashtags", 0); // TODO: Implement in ContentAnalysisService
        featureVector.put("has_url", 0); // TODO: Implement in ContentAnalysisService
        featureVector.put("has_multimedia", postFeats.getHasImage() != null && postFeats.getHasImage() ? 1 : 0);

        // Author features (3)
        // Calculate seniority in years
        double authorSeniority = 0.0;
        if (authorFeats.getAccountAgeDays() != null) {
            authorSeniority = authorFeats.getAccountAgeDays() / 365.0;
        }
        featureVector.put("author_seniority", authorSeniority);

        featureVector.put("author_post_count", authorFeats.getPostCount() != null ? authorFeats.getPostCount() : 0);
        featureVector.put("author_engagement_rate", authorFeats.getEngagementRate() != null ? authorFeats.getEngagementRate() : 0.0);

        // Relationship features (4)
        if (interactionFeats != null) {
            featureVector.put("interaction_count_7d", interactionFeats.getInteractionCount7d());
            featureVector.put("interaction_count_30d", interactionFeats.getInteractionCount30d());
            featureVector.put("hours_since_last_interaction", interactionFeats.getLastInteractionHours());
            featureVector.put("affinity_score", interactionFeats.getAffinityScore());
        } else {
            featureVector.put("interaction_count_7d", 0);
            featureVector.put("interaction_count_30d", 0);
            featureVector.put("hours_since_last_interaction", 999.0);
            featureVector.put("affinity_score", 0.0);
        }

        // Engagement features (6)
        long popularity = safeCount(postFeats.getUpvoteCount())
            + safeCount(postFeats.getDownvoteCount())
            + safeCount(postFeats.getCmtCount())
            + safeCount(postFeats.getShareCount())
            + safeCount(postFeats.getViewCount());

        featureVector.put("popularity", popularity);
        featureVector.put("upvote_count", safeCount(postFeats.getUpvoteCount()));
        featureVector.put("downvote_count", safeCount(postFeats.getDownvoteCount()));
        featureVector.put("comment_count", safeCount(postFeats.getCmtCount()));
        featureVector.put("share_count", safeCount(postFeats.getShareCount()));
        featureVector.put("view_count", safeCount(postFeats.getViewCount()));

        return featureVector;
    }

    private long safeCount(Long value) {
        return value != null ? value : 0L;
    }

    /**
     * Check if AI service is available
     */
    public boolean isAiServiceAvailable() {
        if (!aiServiceEnabled) {
            return false;
        }

        try {
            Map<String, Object> response = restClient.get()
                    .uri("/health")
                    .retrieve()
                    .body(Map.class);
            return response != null && "healthy".equals(response.get("status"));
        } catch (Exception e) {
            log.warn("AI service health check failed", e);
            return false;
        }
    }

}
