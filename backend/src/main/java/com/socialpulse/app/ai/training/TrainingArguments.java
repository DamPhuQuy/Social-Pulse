package com.socialpulse.app.ai.training;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

final class TrainingArguments {
    private final Path submissionsPath;
    private final Path commentsPath;
    private final Path outputPath;
    private final Path metricsOutputPath;
    private final int sampleSize;
    private final int scanLimitPosts;
    private final int scanLimitComments;
    private final int minContentLength;
    private final int nEstimators;
    private final int maxDepth;
    private final int minSamplesLeaf;
    private final int maxThresholds;
    private final double learningRate;
    private final long seed;

    private TrainingArguments(
            Path submissionsPath,
            Path commentsPath,
            Path outputPath,
            Path metricsOutputPath,
            int sampleSize,
            int scanLimitPosts,
            int scanLimitComments,
            int minContentLength,
            int nEstimators,
            int maxDepth,
            int minSamplesLeaf,
            int maxThresholds,
            double learningRate,
            long seed) {
        this.submissionsPath = submissionsPath;
        this.commentsPath = commentsPath;
        this.outputPath = outputPath;
        this.metricsOutputPath = metricsOutputPath;
        this.sampleSize = sampleSize;
        this.scanLimitPosts = scanLimitPosts;
        this.scanLimitComments = scanLimitComments;
        this.minContentLength = minContentLength;
        this.nEstimators = nEstimators;
        this.maxDepth = maxDepth;
        this.minSamplesLeaf = minSamplesLeaf;
        this.maxThresholds = maxThresholds;
        this.learningRate = learningRate;
        this.seed = seed;
    }

    static TrainingArguments parse(String[] args) {
        Map<String, String> values = new HashMap<>();
        for (int index = 0; index < args.length; index++) {
            String argument = args[index];
            if (!argument.startsWith("--")) {
                throw new IllegalArgumentException("Unsupported argument: " + argument);
            }
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + argument);
            }
            values.put(argument.substring(2), args[++index]);
        }

        return new TrainingArguments(
                requiredPath(values, "submissions"),
                optionalPath(values, "comments"),
                requiredPath(values, "output"),
                optionalPath(values, "metrics-output"),
                intValue(values, "sample-size", 12000),
                intValue(values, "scan-limit-posts", 180000),
                intValue(values, "scan-limit-comments", 300000),
                intValue(values, "min-content-length", 20),
                intValue(values, "n-estimators", 16),
                intValue(values, "max-depth", 3),
                intValue(values, "min-samples-leaf", 64),
                intValue(values, "max-thresholds", 16),
                doubleValue(values, "learning-rate", 0.18),
                longValue(values, "seed", 42L));
    }

    void validate() {
        if (!Files.exists(submissionsPath)) {
            throw new IllegalArgumentException("Submissions archive not found: " + submissionsPath);
        }
        if (commentsPath != null && !Files.exists(commentsPath)) {
            throw new IllegalArgumentException("Comments archive not found: " + commentsPath);
        }
        if (sampleSize <= 0
                || scanLimitPosts <= 0
                || scanLimitComments <= 0
                || nEstimators <= 0
                || maxDepth <= 0
                || minSamplesLeaf <= 0
                || maxThresholds <= 0
                || learningRate <= 0.0) {
            throw new IllegalArgumentException("Training arguments must be positive.");
        }
    }

    Path submissionsPath() {
        return submissionsPath;
    }

    Path commentsPath() {
        return commentsPath;
    }

    Path outputPath() {
        return outputPath;
    }

    Path metricsOutputPath() {
        return metricsOutputPath;
    }

    int sampleSize() {
        return sampleSize;
    }

    int scanLimitPosts() {
        return scanLimitPosts;
    }

    int scanLimitComments() {
        return scanLimitComments;
    }

    int minContentLength() {
        return minContentLength;
    }

    int nEstimators() {
        return nEstimators;
    }

    int maxDepth() {
        return maxDepth;
    }

    int minSamplesLeaf() {
        return minSamplesLeaf;
    }

    int maxThresholds() {
        return maxThresholds;
    }

    double learningRate() {
        return learningRate;
    }

    long seed() {
        return seed;
    }

    private static Path requiredPath(Map<String, String> values, String key) {
        String value = values.get(key);
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Missing required argument --" + key);
        }
        return Path.of(value);
    }

    private static Path optionalPath(Map<String, String> values, String key) {
        String value = values.get(key);
        return value == null || value.isBlank() ? null : Path.of(value);
    }

    private static int intValue(Map<String, String> values, String key, int defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Integer.parseInt(value);
    }

    private static long longValue(Map<String, String> values, String key, long defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Long.parseLong(value);
    }

    private static double doubleValue(Map<String, String> values, String key, double defaultValue) {
        String value = values.get(key);
        return value == null ? defaultValue : Double.parseDouble(value);
    }
}
