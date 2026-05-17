package com.socialpulse.app.ai.training;

import java.nio.file.Path;
import java.util.List;
import java.util.Map;

record SubmissionRecord(
        String postId,
        String author,
        Double authorCreatedUtc,
        double createdUtc,
        double retrievedOn,
        int titleLength,
        int bodyLength,
        int score,
        int numComments,
        int numCrossposts,
        boolean hasMultimedia,
        boolean isSharePost,
        double hotScore,
        double upvoteRatio) {
}

final class AuthorAggregate {
    private int postCount;
    private double cumulativePopularity;

    void increment(double popularity) {
        postCount++;
        cumulativePopularity += popularity;
    }

    double postCount() {
        return postCount;
    }

    double averagePopularity() {
        return postCount == 0 ? 0.0 : cumulativePopularity / postCount;
    }

    static AuthorAggregate empty() {
        return new AuthorAggregate();
    }
}

record TrainingRow(String postId, double[] features, double label) {
}

record ScanResult(
        List<SubmissionRecord> sampledPosts,
        Map<String, AuthorAggregate> authorAggregates,
        Map<String, Integer> scanStats) {
}

record TrainingDataset(List<TrainingRow> rows, Map<String, Object> featureStats) {
}

record DatasetSplit(List<TrainingRow> trainRows, List<TrainingRow> validationRows) {
}

record Metrics(double trainRmse, double validationRmse, double trainMae, double validationMae) {
}

record GradientBoostedModel(Map<String, Object> modelDump, Metrics metrics) {
}

record TrainingRunResult(
        Path outputPath,
        String trainedAt,
        Metrics metrics,
        int trainRows,
        int validationRows) {
}

/**
 * Maps viewer → (author → list of interaction timestamps).
 */
record InteractionScanResult(
        Map<String, Map<String, List<Double>>> interactions,
        Map<String, Integer> stats) {
}
