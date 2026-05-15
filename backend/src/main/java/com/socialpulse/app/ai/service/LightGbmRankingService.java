package com.socialpulse.app.ai.service;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.ai.config.LightGbmProperties;
import com.socialpulse.app.ai.lightgbm.LightGbmFeatureVectorizer;
import com.socialpulse.app.ai.lightgbm.LightGbmModel;
import com.socialpulse.app.ai.lightgbm.LightGbmModelArtifact;
import com.socialpulse.app.ai.lightgbm.LightGbmModelScorer;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LightGbmRankingService implements PredictRankingUseCase {
    private final LightGbmProperties properties;
    private final ObjectMapper objectMapper;
    private final ResourceLoader resourceLoader;
    private final LightGbmFeatureVectorizer featureVectorizer;

    private volatile LightGbmModelScorer scorer;

    public LightGbmRankingService(
            LightGbmProperties properties,
            ObjectMapper objectMapper,
            ResourceLoader resourceLoader,
            LightGbmFeatureVectorizer featureVectorizer) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        this.resourceLoader = resourceLoader;
        this.featureVectorizer = featureVectorizer;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        if (!properties.isEnabled() || request == null || request.getFeatures() == null || request.getFeatures().isEmpty()) {
            return List.of();
        }

        if (!properties.getFeatureSchemaVersion().equals(request.getFeatureSchemaVersion())) {
            log.warn("Skipping LightGBM ranking due to feature schema mismatch. expected={}, actual={}",
                    properties.getFeatureSchemaVersion(), request.getFeatureSchemaVersion());
            return List.of();
        }

        LightGbmModelScorer localScorer = getOrLoadScorer();
        if (localScorer == null) {
            return List.of();
        }

        return request.getFeatures().stream()
                .map(features -> toRankingResponse(request, features, localScorer))
                .toList();
    }

    private RankingResponse toRankingResponse(
            RankingRequest request,
            RankingFeatures features,
            LightGbmModelScorer localScorer) {
        double score = localScorer.score(featureVectorizer.toFeatureMap(features));
        return RankingResponse.builder()
                .postId(features.getPostId())
                .score(score)
                .featureSchemaVersion(request.getFeatureSchemaVersion())
                .build();
    }

    private LightGbmModelScorer getOrLoadScorer() {
        if (scorer != null) {
            return scorer;
        }

        synchronized (this) {
            if (scorer != null) {
                return scorer;
            }
            scorer = loadScorer();
            return scorer;
        }
    }

    private LightGbmModelScorer loadScorer() {
        Resource resource = resourceLoader.getResource(properties.getModelLocation());
        if (!resource.exists()) {
            log.warn("LightGBM model resource not found at {}", properties.getModelLocation());
            return null;
        }

        try (InputStream inputStream = resource.getInputStream()) {
            LoadedArtifact loadedArtifact = readArtifact(inputStream);
            if (loadedArtifact == null) {
                return null;
            }

            if (loadedArtifact.featureSchemaVersion() != null
                    && !loadedArtifact.featureSchemaVersion().isBlank()
                    && !properties.getFeatureSchemaVersion().equals(loadedArtifact.featureSchemaVersion())) {
                log.warn("Skipping LightGBM model at {} due to artifact schema mismatch. expected={}, actual={}",
                        properties.getModelLocation(),
                        properties.getFeatureSchemaVersion(),
                        loadedArtifact.featureSchemaVersion());
                return null;
            }

            LightGbmModel model = loadedArtifact.model();
            log.info("Loaded LightGBM model from {} with {} trees, objective={}, schema={}, dataset={}, trainedAt={}",
                    properties.getModelLocation(),
                    model.getTreeInfo().size(),
                    model.getObjectiveName(),
                    loadedArtifact.featureSchemaVersion(),
                    loadedArtifact.trainingDataset(),
                    loadedArtifact.trainedAt());
            return new LightGbmModelScorer(model);
        } catch (IOException | IllegalArgumentException e) {
            log.warn("Failed to load LightGBM model from {}: {}", properties.getModelLocation(), e.getMessage());
            return null;
        }
    }

    private LoadedArtifact readArtifact(InputStream inputStream) throws IOException {
        JsonNode root = objectMapper.readTree(inputStream);

        if (root == null || root.isNull()) {
            throw new IllegalArgumentException("LightGBM artifact is empty");
        }

        if (root.has("tree_info")) {
            LightGbmModel model = objectMapper.treeToValue(root, LightGbmModel.class);
            return new LoadedArtifact(model, properties.getFeatureSchemaVersion(), null, null);
        }

        if (root.has("model_dump")) {
            LightGbmModelArtifact artifact = objectMapper.treeToValue(root, LightGbmModelArtifact.class);
            if (artifact.getModelDump() == null) {
                throw new IllegalArgumentException("LightGBM artifact is missing model_dump");
            }

            return new LoadedArtifact(
                    artifact.getModelDump(),
                    artifact.getFeatureSchemaVersion(),
                    artifact.getTrainingDataset(),
                    artifact.getTrainedAt());
        }

        throw new IllegalArgumentException("Unsupported LightGBM artifact format");
    }

    private record LoadedArtifact(
            LightGbmModel model,
            String featureSchemaVersion,
            String trainingDataset,
            String trainedAt) {
    }
}
