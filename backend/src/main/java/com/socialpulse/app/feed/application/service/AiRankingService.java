package com.socialpulse.app.feed.application.service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

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
    private static final String DEFAULT_SCHEMA_VERSION = "v1";

    private final RestClient restClient;
    private final boolean aiServiceEnabled;
    private final String rankingPath;

    public AiRankingService(
            @Value("${ai.service.url:http://localhost:5000}") String aiServiceUrl,
            @Value("${ai.service.enabled:false}") boolean aiServiceEnabled,
            @Value("${ai.service.timeout-ms:1500}") int timeoutMs,
            @Value("${ai.service.ranking-path:/rank}") String rankingPath) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofMillis(timeoutMs));
        requestFactory.setReadTimeout(Duration.ofMillis(timeoutMs));

        this.restClient = RestClient.builder()
                .baseUrl(aiServiceUrl)
                .requestFactory(requestFactory)
                .build();
        this.aiServiceEnabled = aiServiceEnabled;
        this.rankingPath = rankingPath;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        if (!aiServiceEnabled || request.getFeatures() == null || request.getFeatures().isEmpty()) {
            return List.of();
        }

        try {
            Map<String, Object> predictionRequest = convertToPredictionRequest(request);

            AiRankingResponse aiResponse = restClient.post()
                    .uri(rankingPath)
                    .body(predictionRequest)
                    .retrieve()
                    .body(AiRankingResponse.class);

            if (aiResponse == null || aiResponse.getRankedPosts() == null || aiResponse.getRankedPosts().isEmpty()) {
                log.warn("AI service returned empty ranking response");
                return List.of();
            }

            return aiResponse.getRankedPosts().stream()
                    .map(rankedPost -> RankingResponse.builder()
                            .postId(rankedPost.getPostId())
                            .score(rankedPost.getRankingScore())
                            .featureSchemaVersion(request.getFeatureSchemaVersion())
                            .build())
                    .toList();
        } catch (Exception e) {
            log.warn("AI ranking call failed: {}", e.getMessage());
            return List.of();
        }
    }

    private Map<String, Object> convertToPredictionRequest(RankingRequest request) {
        RankingFeatures firstFeature = request.getFeatures().get(0);
        Long userId = firstFeature.getViewerFeatures() != null
                ? firstFeature.getViewerFeatures().getUserId()
                : null;

        List<Map<String, Object>> posts = new ArrayList<>();
        for (RankingFeatures feature : request.getFeatures()) {
            Map<String, Object> postData = new HashMap<>();
            postData.put("post_id", feature.getPostId());
            postData.put("features", extractFeatureVector(feature));
            posts.add(postData);
        }

        Map<String, Object> predictionRequest = new HashMap<>();
        predictionRequest.put("feature_schema_version",
                request.getFeatureSchemaVersion() != null ? request.getFeatureSchemaVersion() : DEFAULT_SCHEMA_VERSION);
        predictionRequest.put("user_id", userId);
        predictionRequest.put("posts", posts);
        return predictionRequest;
    }

    private Map<String, Object> extractFeatureVector(RankingFeatures features) {
        Map<String, Object> featureVector = new HashMap<>();

        PostFeatures postFeats = features.getPostFeatures();
        UserFeatures authorFeats = features.getAuthorFeatures();
        InteractionFeatures interactionFeats = features.getInteractionFeatures();

        featureVector.put("content_length", postFeats.getContentLength() != null ? postFeats.getContentLength() : 0);
        featureVector.put("has_multimedia", isTrue(postFeats.getHasImage()) ? 1 : 0);
        featureVector.put("is_share_post", isTrue(postFeats.getIsSharePost()) ? 1 : 0);
        featureVector.put("post_age_hours", postFeats.getPostAgeHours() != null ? postFeats.getPostAgeHours() : 0.0);
        featureVector.put("hot_score", postFeats.getHotScore() != null ? postFeats.getHotScore() : 0.0);
        featureVector.put("upvote_ratio", postFeats.getUpvoteRatio() != null ? postFeats.getUpvoteRatio() : 0.5);

        double authorSeniority = 0.0;
        if (authorFeats != null && authorFeats.getAccountAgeDays() != null) {
            authorSeniority = authorFeats.getAccountAgeDays() / 365.0;
        }
        featureVector.put("author_seniority", authorSeniority);
        featureVector.put("author_post_count",
                authorFeats != null && authorFeats.getPostCount() != null ? authorFeats.getPostCount() : 0);
        featureVector.put("author_engagement_rate",
                authorFeats != null && authorFeats.getEngagementRate() != null ? authorFeats.getEngagementRate() : 0.0);

        featureVector.put("interaction_count_7d",
                interactionFeats != null ? interactionFeats.getInteractionCount7d() : 0);
        featureVector.put("interaction_count_30d",
                interactionFeats != null ? interactionFeats.getInteractionCount30d() : 0);
        featureVector.put("hours_since_last_interaction",
                interactionFeats != null ? interactionFeats.getLastInteractionHours() : 999.0);
        featureVector.put("affinity_score",
                interactionFeats != null ? interactionFeats.getAffinityScore() : 0.0);

        featureVector.put("upvote_count", safeCount(postFeats.getUpvoteCount()));
        featureVector.put("downvote_count", safeCount(postFeats.getDownvoteCount()));
        featureVector.put("comment_count", safeCount(postFeats.getCmtCount()));
        featureVector.put("share_count", safeCount(postFeats.getShareCount()));
        featureVector.put("view_count", safeCount(postFeats.getViewCount()));
        featureVector.put("popularity",
                safeCount(postFeats.getUpvoteCount())
                        + safeCount(postFeats.getDownvoteCount())
                        + safeCount(postFeats.getCmtCount())
                        + safeCount(postFeats.getShareCount())
                        + safeCount(postFeats.getViewCount()));

        return featureVector;
    }

    private boolean isTrue(Boolean value) {
        return value != null && value;
    }

    private long safeCount(Long value) {
        return value != null ? value : 0L;
    }

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
            log.warn("AI service health check failed: {}", e.getMessage());
            return false;
        }
    }
}
