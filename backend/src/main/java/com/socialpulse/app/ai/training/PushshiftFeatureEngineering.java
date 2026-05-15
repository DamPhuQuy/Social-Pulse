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
            Map<String, AuthorAggregate> authorAggregates) {
        double referenceUtc = sampledPosts.stream()
                .mapToDouble(SubmissionRecord::retrievedOn)
                .max()
                .orElse(0.0);

        double popularityTotal = 0.0;
        List<TrainingRow> rows = new ArrayList<>(sampledPosts.size());

        for (SubmissionRecord record : sampledPosts) {
            AuthorAggregate aggregate = authorAggregates.getOrDefault(record.author(), AuthorAggregate.empty());
            double popularity = PushshiftDatasetScanner.popularity(record.score(), record.numComments(), record.numCrossposts());
            popularityTotal += popularity;

            double authorSeniority = 0.0;
            if (record.authorCreatedUtc() != null && record.authorCreatedUtc() > 0) {
                authorSeniority = Math.max(record.createdUtc() - record.authorCreatedUtc(), 0.0) / SECONDS_PER_YEAR;
            }

            rows.add(new TrainingRow(
                    record.postId(),
                    new double[] {
                            record.titleLength() + record.bodyLength(),
                            record.hasMultimedia() ? 1.0 : 0.0,
                            record.isSharePost() ? 1.0 : 0.0,
                            Math.max(referenceUtc - record.createdUtc(), 0.0) / SECONDS_PER_HOUR,
                            record.hotScore(),
                            LightGbmFeatureSchema.DEFAULT_UPVOTE_RATIO,
                            authorSeniority,
                            aggregate.postCount(),
                            aggregate.averagePopularity(),
                            0.0,
                            0.0,
                            LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS,
                            0.0,
                            Math.max(record.score(), 0),
                            0.0,
                            record.numComments(),
                            record.numCrossposts(),
                            0.0,
                            popularity
                    },
                    Math.log1p(popularity)));
        }

        Map<String, Object> featureStats = new LinkedHashMap<>();
        featureStats.put("average_popularity", popularityTotal / Math.max(rows.size(), 1));
        featureStats.put("reference_utc", referenceUtc);
        return new TrainingDataset(List.copyOf(rows), featureStats);
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
