package com.socialpulse.app.ai.training;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.socialpulse.app.ai.shared.LightGbmFeatureSchema;

final class PushshiftFeatureEngineering {
    private static final double SECONDS_PER_HOUR = 3600.0;
    private static final double SECONDS_PER_DAY = 86400.0;
    private static final double SECONDS_PER_YEAR = 365.0 * SECONDS_PER_DAY;

    TrainingDataset buildTrainingDataset(
            List<SubmissionRecord> sampledPosts,
            Map<String, AuthorAggregate> authorAggregates,
            Map<String, Map<String, List<Double>>> interactions,
            int negativeSamplesPerPost) {
        double referenceUtc = sampledPosts.stream()
                .mapToDouble(SubmissionRecord::retrievedOn)
                .max()
                .orElse(0.0);

        // Build total comment counts per viewer for affinity normalization
        Map<String, Integer> viewerTotalInteractions = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<Double>>> entry : interactions.entrySet()) {
            int total = entry.getValue().values().stream().mapToInt(List::size).sum();
            viewerTotalInteractions.put(entry.getKey(), total);
        }

        List<TrainingRow> rows = new ArrayList<>();

        for (SubmissionRecord record : sampledPosts) {
            AuthorAggregate aggregate = authorAggregates.getOrDefault(record.author(), AuthorAggregate.empty());
            double popularity = PushshiftDatasetScanner.popularity(record.score(), record.numComments(), record.numCrossposts());
            double[] baseFeatures = buildBaseFeatures(record, aggregate, referenceUtc);

            // Positive rows: viewers who commented on this author's posts
            Map<String, List<Double>> authorInteractors = findViewersForAuthor(interactions, record.author());
            for (Map.Entry<String, List<Double>> viewer : authorInteractors.entrySet()) {
                double[] interactionFeatures = computeInteractionFeatures(
                        viewer.getValue(), record.createdUtc(),
                        viewerTotalInteractions.getOrDefault(viewer.getKey(), 1));
                rows.add(new TrainingRow(record.postId(), mergeFeatures(baseFeatures, interactionFeatures), Math.log1p(popularity)));
            }

            // Negative rows: viewers who interacted with OTHER authors but not this one
            List<String> negativeViewers = findNegativeViewers(interactions, record.author(), negativeSamplesPerPost);
            for (String viewer : negativeViewers) {
                double[] zeroInteraction = new double[] { 0.0, 0.0, LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0 };
                rows.add(new TrainingRow(record.postId(), mergeFeatures(baseFeatures, zeroInteraction), 0.0));
            }

            // Fallback: if no interaction data, keep one row with placeholders (content-only signal)
            if (authorInteractors.isEmpty() && negativeViewers.isEmpty()) {
                double[] placeholder = new double[] { 0.0, 0.0, LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0 };
                rows.add(new TrainingRow(record.postId(), mergeFeatures(baseFeatures, placeholder), Math.log1p(popularity)));
            }
        }

        Map<String, Object> featureStats = new LinkedHashMap<>();
        featureStats.put("total_training_rows", rows.size());
        featureStats.put("reference_utc", referenceUtc);
        return new TrainingDataset(List.copyOf(rows), featureStats);
    }

    private double[] buildBaseFeatures(SubmissionRecord record, AuthorAggregate aggregate, double referenceUtc) {
        double authorSeniority = 0.0;
        if (record.authorCreatedUtc() != null && record.authorCreatedUtc() > 0) {
            authorSeniority = Math.max(record.createdUtc() - record.authorCreatedUtc(), 0.0) / SECONDS_PER_YEAR;
        }
        double popularity = PushshiftDatasetScanner.popularity(record.score(), record.numComments(), record.numCrossposts());
        return new double[] {
                record.titleLength() + record.bodyLength(),
                record.hasMultimedia() ? 1.0 : 0.0,
                record.isSharePost() ? 1.0 : 0.0,
                Math.max(referenceUtc - record.createdUtc(), 0.0) / SECONDS_PER_HOUR,
                record.hotScore(),
                record.upvoteRatio(),
                authorSeniority,
                aggregate.postCount(),
                aggregate.averagePopularity(),
                // slots 9-12 filled by mergeFeatures
                0.0, 0.0, 0.0, 0.0,
                Math.max(record.score(), 0),
                0.0,
                record.numComments(),
                record.numCrossposts(),
                0.0,
                popularity
        };
    }

    private double[] mergeFeatures(double[] base, double[] interactionFeatures) {
        double[] merged = base.clone();
        merged[9] = interactionFeatures[0];   // interaction_count_7d
        merged[10] = interactionFeatures[1];  // interaction_count_30d
        merged[11] = interactionFeatures[2];  // hours_since_last_interaction
        merged[12] = interactionFeatures[3];  // affinity_score
        return merged;
    }

    double[] computeInteractionFeatures(List<Double> timestamps, double postCreatedUtc, int viewerTotal) {
        long count7d = 0;
        long count30d = 0;
        double latestTimestamp = 0.0;

        double sevenDaysBefore = postCreatedUtc - (7 * SECONDS_PER_DAY);
        double thirtyDaysBefore = postCreatedUtc - (30 * SECONDS_PER_DAY);

        for (double ts : timestamps) {
            if (ts >= sevenDaysBefore && ts < postCreatedUtc) {
                count7d++;
            }
            if (ts >= thirtyDaysBefore && ts < postCreatedUtc) {
                count30d++;
            }
            if (ts > latestTimestamp && ts < postCreatedUtc) {
                latestTimestamp = ts;
            }
        }

        double hoursSinceLast = latestTimestamp > 0
                ? (postCreatedUtc - latestTimestamp) / SECONDS_PER_HOUR
                : LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS;
        double affinity = viewerTotal > 0 ? (double) count30d / viewerTotal : 0.0;

        return new double[] { count7d, count30d, hoursSinceLast, affinity };
    }

    private Map<String, List<Double>> findViewersForAuthor(
            Map<String, Map<String, List<Double>>> interactions, String author) {
        Map<String, List<Double>> result = new LinkedHashMap<>();
        for (Map.Entry<String, Map<String, List<Double>>> entry : interactions.entrySet()) {
            List<Double> timestamps = entry.getValue().get(author);
            if (timestamps != null && !timestamps.isEmpty()) {
                result.put(entry.getKey(), timestamps);
            }
        }
        return result;
    }

    private List<String> findNegativeViewers(
            Map<String, Map<String, List<Double>>> interactions, String author, int limit) {
        List<String> negatives = new ArrayList<>();
        for (Map.Entry<String, Map<String, List<Double>>> entry : interactions.entrySet()) {
            if (!entry.getValue().containsKey(author)) {
                negatives.add(entry.getKey());
                if (negatives.size() >= limit) {
                    break;
                }
            }
        }
        return negatives;
    }

    DatasetSplit splitRows(List<TrainingRow> rows) {
        List<TrainingRow> trainRows = new ArrayList<>();
        List<TrainingRow> validationRows = new ArrayList<>();

        for (TrainingRow row : rows) {
            if (bucketForPostId(row.postId()) == 0) {
                validationRows.add(row);
            } else {
                trainRows.add(row);
            }
        }

        return new DatasetSplit(List.copyOf(trainRows), List.copyOf(validationRows));
    }

    private int bucketForPostId(String postId) {
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] hashed = digest.digest(postId.getBytes(StandardCharsets.UTF_8));
            int value = ((hashed[0] & 0xff) << 24)
                    | ((hashed[1] & 0xff) << 16)
                    | ((hashed[2] & 0xff) << 8)
                    | (hashed[3] & 0xff);
            return Math.floorMod(value, 5);
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("MD5 digest is unavailable", exception);
        }
    }
}
