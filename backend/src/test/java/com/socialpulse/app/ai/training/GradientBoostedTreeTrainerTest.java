package com.socialpulse.app.ai.training;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.junit.jupiter.api.Test;

import com.socialpulse.app.ai.shared.LightGbmFeatureSchema;

class GradientBoostedTreeTrainerTest {

    private final GradientBoostedTreeTrainer trainer = new GradientBoostedTreeTrainer();

    @Test
    void trainProducesModelWithCorrectStructure() {
        List<TrainingRow> trainRows = generateRows(200, 42L);
        List<TrainingRow> validationRows = generateRows(50, 99L);

        TrainingArguments args = buildArgs(8, 3, 16, 16, 0.1);
        GradientBoostedModel model = trainer.train(args, trainRows, validationRows);

        assertNotNull(model.modelDump());
        assertNotNull(model.metrics());

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> treeInfo = (List<Map<String, Object>>) model.modelDump().get("tree_info");
        // 1 bias tree + n_estimators
        assertEquals(9, treeInfo.size());
        assertEquals("regression", model.modelDump().get("objective"));
        assertEquals(LightGbmFeatureSchema.FEATURE_ORDER, model.modelDump().get("feature_names"));
    }

    @Test
    void trainReducesErrorOverIterations() {
        List<TrainingRow> trainRows = generateRows(500, 42L);
        List<TrainingRow> validationRows = generateRows(100, 99L);

        TrainingArguments fewTrees = buildArgs(2, 3, 32, 16, 0.18);
        GradientBoostedModel modelFew = trainer.train(fewTrees, trainRows, validationRows);

        TrainingArguments moreTrees = buildArgs(16, 3, 32, 16, 0.18);
        GradientBoostedModel modelMore = trainer.train(moreTrees, trainRows, validationRows);

        assertTrue(modelMore.metrics().trainRmse() <= modelFew.metrics().trainRmse(),
                "More trees should reduce or maintain train RMSE");
    }

    @Test
    void trainMetricsAreFiniteAndPositive() {
        List<TrainingRow> trainRows = generateRows(300, 42L);
        List<TrainingRow> validationRows = generateRows(80, 99L);

        TrainingArguments args = buildArgs(8, 3, 32, 16, 0.18);
        GradientBoostedModel model = trainer.train(args, trainRows, validationRows);

        assertTrue(Double.isFinite(model.metrics().trainRmse()));
        assertTrue(Double.isFinite(model.metrics().validationRmse()));
        assertTrue(Double.isFinite(model.metrics().trainMae()));
        assertTrue(Double.isFinite(model.metrics().validationMae()));
        assertTrue(model.metrics().trainRmse() >= 0);
        assertTrue(model.metrics().validationRmse() >= 0);
    }

    private List<TrainingRow> generateRows(int count, long seed) {
        Random random = new Random(seed);
        int featureCount = LightGbmFeatureSchema.FEATURE_ORDER.size();
        List<TrainingRow> rows = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            double[] features = new double[featureCount];
            for (int f = 0; f < featureCount; f++) {
                features[f] = random.nextDouble() * 100;
            }
            // Label correlated with feature[18] (popularity)
            double label = Math.log1p(features[18]);
            rows.add(new TrainingRow("post_" + i, features, label));
        }
        return rows;
    }

    private TrainingArguments buildArgs(int nEstimators, int maxDepth, int minSamplesLeaf, int maxThresholds, double learningRate) {
        return new TrainingArguments(
                java.nio.file.Path.of("/tmp/dummy"),
                null,
                java.nio.file.Path.of("/tmp/out.json"),
                null,
                1000, 10000, 10000, 20,
                nEstimators, maxDepth, minSamplesLeaf, maxThresholds, learningRate, 42L);
    }
}
