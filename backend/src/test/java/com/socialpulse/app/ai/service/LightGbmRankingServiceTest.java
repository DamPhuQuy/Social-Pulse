package com.socialpulse.app.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.DefaultResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.ai.config.LightGbmProperties;
import com.socialpulse.app.ai.lightgbm.LightGbmFeatureVectorizer;
import com.socialpulse.app.feed.application.dto.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.UserFeatures;

class LightGbmRankingServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @TempDir
    Path tempDir;

    @Test
    void predictsRankingScoresFromLocalModelFile() throws Exception {
        Path modelPath = tempDir.resolve("lightgbm-ranking-model.json");
        Files.writeString(modelPath, """
                {
                  "objective": "lambdarank",
                  "feature_names": ["hot_score", "affinity_score"],
                  "tree_info": [
                    {
                      "shrinkage": 1.0,
                      "tree_structure": {
                        "split_feature": 0,
                        "threshold": 10.0,
                        "decision_type": "<=",
                        "default_left": true,
                        "left_child": { "leaf_value": 0.1 },
                        "right_child": { "leaf_value": 0.8 }
                      }
                    },
                    {
                      "shrinkage": 1.0,
                      "tree_structure": {
                        "split_feature": 1,
                        "threshold": 1.0,
                        "decision_type": "<=",
                        "default_left": true,
                        "left_child": { "leaf_value": 0.0 },
                        "right_child": { "leaf_value": 0.4 }
                      }
                    }
                  ]
                }
                """);

        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setModelLocation(modelPath.toUri().toString());
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        // Expected scores were computed from the same dumped tree ensemble structure
        // and should match Python LightGBM inference for this fixture model.
        RankingRequest request = RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(
                        rankingFeatures(100L, 6.0, 0.5),
                        rankingFeatures(200L, 15.0, 2.0)))
                .build();

        var responses = service.predictScores(request);

        assertEquals(2, responses.size());
        assertEquals(100L, responses.get(0).getPostId());
        assertEquals(0.1, responses.get(0).getScore(), 1e-9);
        assertEquals(200L, responses.get(1).getPostId());
        assertEquals(1.2, responses.get(1).getScore(), 1e-9);
    }

    @Test
    void predictsRankingScoresFromWrappedArtifactFile() throws Exception {
        Path modelPath = tempDir.resolve("lightgbm-ranking-artifact.json");
        Files.writeString(modelPath, """
                {
                  "artifact_version": "1",
                  "feature_schema_version": "v1",
                  "training_dataset": "pushshift_reddit",
                  "trained_at": "2026-05-15T00:00:00Z",
                  "label_strategy": "implicit_pairwise",
                  "model_dump": {
                    "objective": "lambdarank",
                    "feature_names": ["hot_score", "affinity_score"],
                    "tree_info": [
                      {
                        "shrinkage": 1.0,
                        "tree_structure": {
                          "split_feature": 0,
                          "threshold": 10.0,
                          "decision_type": "<=",
                          "default_left": true,
                          "left_child": { "leaf_value": 0.2 },
                          "right_child": { "leaf_value": 0.9 }
                        }
                      }
                    ]
                  }
                }
                """);

        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setModelLocation(modelPath.toUri().toString());
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        RankingRequest request = RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(
                        rankingFeatures(100L, 6.0, 0.5),
                        rankingFeatures(200L, 15.0, 2.0)))
                .build();

        var responses = service.predictScores(request);

        assertEquals(2, responses.size());
        assertEquals(0.2, responses.get(0).getScore(), 1e-9);
        assertEquals(0.9, responses.get(1).getScore(), 1e-9);
    }

    @Test
    void returnsEmptyWhenDisabled() {
        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(false);
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        var responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(100L, 6.0, 0.5)))
                .build());

        assertEquals(0, responses.size());
    }

    @Test
    void returnsEmptyWhenArtifactSchemaDoesNotMatchConfiguredSchema() throws Exception {
        Path modelPath = tempDir.resolve("lightgbm-ranking-artifact-mismatch.json");
        Files.writeString(modelPath, """
                {
                  "feature_schema_version": "v2",
                  "model_dump": {
                    "objective": "lambdarank",
                    "feature_names": ["hot_score"],
                    "tree_info": [
                      {
                        "shrinkage": 1.0,
                        "tree_structure": {
                          "split_feature": 0,
                          "threshold": 10.0,
                          "decision_type": "<=",
                          "default_left": true,
                          "left_child": { "leaf_value": 0.2 },
                          "right_child": { "leaf_value": 0.9 }
                        }
                      }
                    ]
                  }
                }
                """);

        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setModelLocation(modelPath.toUri().toString());
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        var responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(100L, 6.0, 0.5)))
                .build());

        assertEquals(0, responses.size());
    }

    @Test
    void returnsEmptyWhenModelPathIsInvalid() {
        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setModelLocation("classpath:ai/does-not-exist.json");
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        var responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(100L, 6.0, 0.5)))
                .build());

        assertEquals(0, responses.size());
    }

    @Test
    void loadsBundledClasspathArtifact() {
        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService service = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        var responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(100L, 12.0, 1.0)))
                .build());

        assertEquals(1, responses.size());
        assertTrue(Double.isFinite(responses.get(0).getScore()));
    }

    private RankingFeatures rankingFeatures(Long postId, double hotScore, double affinityScore) {
        return RankingFeatures.builder()
                .postId(postId)
                .postFeatures(PostFeatures.builder()
                        .hotScore(hotScore)
                        .upvoteRatio(0.7)
                        .contentLength(120)
                        .postAgeHours(3.0)
                        .hasImage(true)
                        .isSharePost(false)
                        .upvoteCount(20L)
                        .downvoteCount(2L)
                        .cmtCount(5L)
                        .shareCount(1L)
                        .viewCount(100L)
                        .build())
                .authorFeatures(UserFeatures.builder()
                        .accountAgeDays(365L)
                        .postCount(50L)
                        .engagementRate(0.15)
                        .build())
                .interactionFeatures(InteractionFeatures.builder()
                        .interactionCount7d(4)
                        .interactionCount30d(10)
                        .lastInteractionHours(12.0)
                        .affinityScore(affinityScore)
                        .build())
                .build();
    }
}
