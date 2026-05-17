package com.socialpulse.app.ai.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Test;

import com.socialpulse.app.ai.shared.LightGbmFeatureSchema;

class PushshiftFeatureEngineeringTest {

    private final PushshiftFeatureEngineering featureEngineering = new PushshiftFeatureEngineering();

    @Test
    void buildTrainingDatasetUsesActualUpvoteRatio() {
        SubmissionRecord record = new SubmissionRecord(
                "abc123", "testuser", 1500000000.0,
                1560000000.0, 1560000100.0,
                50, 100, 42, 10, 2,
                true, false, 5.5, 0.85);

        AuthorAggregate aggregate = new AuthorAggregate();
        aggregate.increment(50.0);

        TrainingDataset dataset = featureEngineering.buildTrainingDataset(
                List.of(record), Map.of("testuser", aggregate), Map.of(), 0);

        assertEquals(1, dataset.rows().size());
        double[] features = dataset.rows().get(0).features();

        // Index 5 = upvote_ratio — should be 0.85, not 0.5
        assertEquals(0.85, features[5], 1e-9);
    }

    @Test
    void buildTrainingDatasetComputesCorrectFeatures() {
        double createdUtc = 1560000000.0;
        double retrievedOn = 1560003600.0; // 1 hour later
        SubmissionRecord record = new SubmissionRecord(
                "post1", "author1", 1550000000.0,
                createdUtc, retrievedOn,
                30, 70, 100, 20, 5,
                false, true, 3.0, 0.92);

        AuthorAggregate aggregate = new AuthorAggregate();
        aggregate.increment(80.0);
        aggregate.increment(120.0);

        TrainingDataset dataset = featureEngineering.buildTrainingDataset(
                List.of(record), Map.of("author1", aggregate), Map.of(), 0);

        double[] features = dataset.rows().get(0).features();
        int featureCount = LightGbmFeatureSchema.FEATURE_ORDER.size();
        assertEquals(featureCount, features.length);

        // content_length = 30 + 70
        assertEquals(100.0, features[0], 1e-9);
        // has_multimedia = false
        assertEquals(0.0, features[1], 1e-9);
        // is_share_post = true
        assertEquals(1.0, features[2], 1e-9);
        // post_age_hours = (retrievedOn - createdUtc) / 3600 = 1.0
        assertEquals(1.0, features[3], 1e-9);
        // hot_score
        assertEquals(3.0, features[4], 1e-9);
        // upvote_ratio
        assertEquals(0.92, features[5], 1e-9);
        // author_seniority = (1560000000 - 1550000000) / (365*86400)
        double expectedSeniority = 10000000.0 / (365.0 * 86400.0);
        assertEquals(expectedSeniority, features[6], 1e-6);
        // author_post_count
        assertEquals(2.0, features[7], 1e-9);
        // author_engagement_rate (average popularity)
        assertEquals(100.0, features[8], 1e-9);
        // upvote_count = score
        assertEquals(100.0, features[13], 1e-9);
        // comment_count
        assertEquals(20.0, features[15], 1e-9);
        // share_count = num_crossposts
        assertEquals(5.0, features[16], 1e-9);
        // popularity = 100 + 20 + 5
        assertEquals(125.0, features[18], 1e-9);

        // label = log1p(popularity)
        assertEquals(Math.log1p(125.0), dataset.rows().get(0).label(), 1e-9);
    }

    @Test
    void splitRowsProducesDeterministicSplit() {
        List<SubmissionRecord> records = List.of(
                record("aaa"), record("bbb"), record("ccc"),
                record("ddd"), record("eee"), record("fff"),
                record("ggg"), record("hhh"), record("iii"), record("jjj"));

        TrainingDataset dataset = featureEngineering.buildTrainingDataset(
                records, Map.of(), Map.of(), 0);

        DatasetSplit split = featureEngineering.splitRows(dataset.rows());

        assertFalse(split.trainRows().isEmpty());
        assertFalse(split.validationRows().isEmpty());
        assertEquals(dataset.rows().size(), split.trainRows().size() + split.validationRows().size());

        // Re-run should produce same split
        DatasetSplit split2 = featureEngineering.splitRows(dataset.rows());
        assertEquals(split.trainRows().size(), split2.trainRows().size());
        assertEquals(split.validationRows().size(), split2.validationRows().size());
    }

    @Test
    void splitRowsApproximately80_20() {
        // Generate enough records to get a statistically meaningful split
        List<SubmissionRecord> records = new java.util.ArrayList<>();
        for (int i = 0; i < 1000; i++) {
            records.add(record("post_" + i));
        }

        TrainingDataset dataset = featureEngineering.buildTrainingDataset(records, Map.of(), Map.of(), 0);
        DatasetSplit split = featureEngineering.splitRows(dataset.rows());

        double validationRatio = (double) split.validationRows().size() / dataset.rows().size();
        // MD5 mod 5 → ~20% validation
        assertTrue(validationRatio > 0.15 && validationRatio < 0.25,
                "Validation ratio should be ~20%, got " + validationRatio);
    }

    private SubmissionRecord record(String postId) {
        return new SubmissionRecord(
                postId, "user", null,
                1560000000.0, 1560000000.0,
                30, 30, 10, 5, 0,
                false, false, 1.0, 0.75);
    }
}
