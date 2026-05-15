package com.socialpulse.app.ai.training;

import java.io.IOException;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.LinkedHashMap;
import java.util.Map;

import com.socialpulse.app.ai.shared.LightGbmFeatureSchema;

final class PushshiftTrainingPipeline {
    private static final String DATASET_NAME = "pushshift_reddit_apr2019";
    private static final String LABEL_STRATEGY = "log_popularity_proxy";

    private final PushshiftDatasetScanner datasetScanner = new PushshiftDatasetScanner();
    private final PushshiftFeatureEngineering featureEngineering = new PushshiftFeatureEngineering();
    private final GradientBoostedTreeTrainer treeTrainer = new GradientBoostedTreeTrainer();

    TrainingRunResult run(TrainingArguments arguments) throws IOException {
        arguments.validate();

        ScanResult scanResult = datasetScanner.scanSubmissions(arguments);
        if (scanResult.sampledPosts().size() < 512) {
            throw new IllegalStateException("Not enough cleaned submissions to train a model: " + scanResult.sampledPosts().size());
        }

        Map<String, Integer> commentStats = arguments.getCommentsPath() != null
                ? datasetScanner.scanComments(arguments.getCommentsPath(), scanResult.sampledPosts(), arguments.getScanLimitComments())
                : Map.of(
                        "comments_scanned", 0,
                        "matched_sample_posts", 0);

        TrainingDataset dataset = featureEngineering.buildTrainingDataset(
                scanResult.sampledPosts(),
                scanResult.authorAggregates());
        DatasetSplit split = featureEngineering.splitRows(dataset.rows());
        if (split.trainRows().isEmpty() || split.validationRows().isEmpty()) {
            throw new IllegalStateException("Unable to build both train and validation splits.");
        }

        GradientBoostedModel model = treeTrainer.train(arguments, split.trainRows(), split.validationRows());

        String trainedAt = DateTimeFormatter.ISO_OFFSET_DATE_TIME.format(OffsetDateTime.now(ZoneOffset.UTC));
        Map<String, Object> trainingSummary = buildTrainingSummary(
                arguments,
                scanResult.scanStats(),
                commentStats,
                dataset.featureStats(),
                split.trainRows().size(),
                split.validationRows().size(),
                model.metrics());

        TrainingJsonSupport.writeJson(arguments.getOutputPath(), buildArtifact(trainedAt, trainingSummary, model));
        if (arguments.getMetricsOutputPath() != null) {
            TrainingJsonSupport.writeJson(arguments.getMetricsOutputPath(), trainingSummary);
        }

        return new TrainingRunResult(
                arguments.getOutputPath(),
                trainedAt,
                model.metrics(),
                split.trainRows().size(),
                split.validationRows().size());
    }

    private Map<String, Object> buildTrainingSummary(
            TrainingArguments arguments,
            Map<String, Integer> scanStats,
            Map<String, Integer> commentStats,
            Map<String, Object> featureStats,
            int trainRows,
            int validationRows,
            Metrics metrics) {
        Map<String, Object> summary = new LinkedHashMap<>();
        summary.put("scan_stats", scanStats);
        summary.put("comment_stats", commentStats);
        summary.put("feature_stats", featureStats);
        summary.put("train_rows", trainRows);
        summary.put("validation_rows", validationRows);

        Map<String, Object> metricMap = new LinkedHashMap<>();
        metricMap.put("train_rmse", metrics.trainRmse());
        metricMap.put("validation_rmse", metrics.validationRmse());
        metricMap.put("train_mae", metrics.trainMae());
        metricMap.put("validation_mae", metrics.validationMae());
        summary.put("metrics", metricMap);

        Map<String, Object> hyperparameters = new LinkedHashMap<>();
        hyperparameters.put("sample_size", arguments.getSampleSize());
        hyperparameters.put("scan_limit_posts", arguments.getScanLimitPosts());
        hyperparameters.put("scan_limit_comments", arguments.getScanLimitComments());
        hyperparameters.put("n_estimators", arguments.getNEstimators());
        hyperparameters.put("max_depth", arguments.getMaxDepth());
        hyperparameters.put("min_samples_leaf", arguments.getMinSamplesLeaf());
        hyperparameters.put("max_thresholds", arguments.getMaxThresholds());
        hyperparameters.put("learning_rate", arguments.getLearningRate());
        hyperparameters.put("seed", arguments.getSeed());
        summary.put("hyperparameters", hyperparameters);
        return summary;
    }

    private Map<String, Object> buildArtifact(
            String trainedAt,
            Map<String, Object> trainingSummary,
            GradientBoostedModel model) {
        Map<String, Object> artifact = new LinkedHashMap<>();
        artifact.put("artifact_version", "1");
        artifact.put("feature_schema_version", LightGbmFeatureSchema.DEFAULT_SCHEMA_VERSION);
        artifact.put("training_dataset", DATASET_NAME);
        artifact.put("trained_at", trainedAt);
        artifact.put("label_strategy", LABEL_STRATEGY);
        artifact.put("training_summary", trainingSummary);
        artifact.put("model_dump", model.modelDump());
        return artifact;
    }
}
