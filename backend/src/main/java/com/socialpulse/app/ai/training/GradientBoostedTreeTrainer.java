package com.socialpulse.app.ai.training;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.socialpulse.app.ai.lightgbm.LightGbmFeatureSchema;

final class GradientBoostedTreeTrainer {
    GradientBoostedModel train(
            TrainingArguments arguments,
            List<TrainingRow> trainRows,
            List<TrainingRow> validationRows) {
        double[] trainTargets = trainRows.stream().mapToDouble(TrainingRow::label).toArray();
        double[] validationTargets = validationRows.stream().mapToDouble(TrainingRow::label).toArray();

        double bias = mean(trainTargets);
        List<Map<String, Object>> treeInfo = new ArrayList<>();
        treeInfo.add(treeInfo(1.0, leafNode(bias)));

        double[] trainPredictions = constantPrediction(trainRows.size(), bias);
        double[] validationPredictions = constantPrediction(validationRows.size(), bias);

        for (int estimator = 0; estimator < arguments.nEstimators(); estimator++) {
            double[] residuals = new double[trainTargets.length];
            for (int index = 0; index < trainTargets.length; index++) {
                residuals[index] = trainTargets[index] - trainPredictions[index];
            }

            int[] indices = new int[trainRows.size()];
            for (int index = 0; index < indices.length; index++) {
                indices[index] = index;
            }

            TreeNode tree = buildTree(
                    trainRows,
                    residuals,
                    indices,
                    0,
                    arguments.maxDepth(),
                    arguments.minSamplesLeaf(),
                    arguments.maxThresholds());

            treeInfo.add(treeInfo(arguments.learningRate(), tree.toArtifactMap()));

            for (int index = 0; index < trainRows.size(); index++) {
                trainPredictions[index] += arguments.learningRate() * tree.predict(trainRows.get(index).features());
            }
            for (int index = 0; index < validationRows.size(); index++) {
                validationPredictions[index] += arguments.learningRate() * tree.predict(validationRows.get(index).features());
            }
        }

        Metrics metrics = new Metrics(
                rmse(trainTargets, trainPredictions),
                rmse(validationTargets, validationPredictions),
                mae(trainTargets, trainPredictions),
                mae(validationTargets, validationPredictions));

        Map<String, Object> modelDump = new LinkedHashMap<>();
        modelDump.put("objective", "regression");
        modelDump.put("average_output", false);
        modelDump.put("feature_names", LightGbmFeatureSchema.FEATURE_ORDER);
        modelDump.put("tree_info", treeInfo);
        return new GradientBoostedModel(modelDump, metrics);
    }

    private TreeNode buildTree(
            List<TrainingRow> rows,
            double[] targets,
            int[] indices,
            int depth,
            int maxDepth,
            int minSamplesLeaf,
            int maxThresholds) {
        double prediction = mean(indices, targets);
        if (depth >= maxDepth || indices.length < minSamplesLeaf * 2) {
            return new Leaf(prediction);
        }

        SplitCandidate best = findBestSplit(rows, targets, indices, minSamplesLeaf, maxThresholds);
        if (best == null) {
            return new Leaf(prediction);
        }

        return new Split(
                best.featureIndex(),
                best.threshold(),
                buildTree(rows, targets, best.leftIndices(), depth + 1, maxDepth, minSamplesLeaf, maxThresholds),
                buildTree(rows, targets, best.rightIndices(), depth + 1, maxDepth, minSamplesLeaf, maxThresholds));
    }

    private SplitCandidate findBestSplit(
            List<TrainingRow> rows,
            double[] targets,
            int[] indices,
            int minSamplesLeaf,
            int maxThresholds) {
        SplitCandidate best = null;
        int featureCount = LightGbmFeatureSchema.FEATURE_ORDER.size();

        for (int featureIndex = 0; featureIndex < featureCount; featureIndex++) {
            int[] sortedIndices = Arrays.copyOf(indices, indices.length);
            sortByFeature(sortedIndices, rows, featureIndex);

            double[] featureValues = new double[sortedIndices.length];
            for (int position = 0; position < sortedIndices.length; position++) {
                featureValues[position] = rows.get(sortedIndices[position]).features()[featureIndex];
            }

            double[] uniqueValues = distinctSortedValues(featureValues);
            if (uniqueValues.length <= 1) {
                continue;
            }

            double[] thresholds = candidateThresholds(uniqueValues, maxThresholds);
            for (double threshold : thresholds) {
                int splitPosition = upperBound(featureValues, threshold);
                int leftCount = splitPosition;
                int rightCount = sortedIndices.length - splitPosition;
                if (leftCount < minSamplesLeaf || rightCount < minSamplesLeaf) {
                    continue;
                }

                double loss = squaredError(sortedIndices, 0, splitPosition, targets)
                        + squaredError(sortedIndices, splitPosition, sortedIndices.length, targets);

                if (best == null || loss < best.loss()) {
                    best = new SplitCandidate(
                            featureIndex,
                            threshold,
                            loss,
                            Arrays.copyOfRange(sortedIndices, 0, splitPosition),
                            Arrays.copyOfRange(sortedIndices, splitPosition, sortedIndices.length));
                }
            }
        }

        return best;
    }

    private void sortByFeature(int[] indices, List<TrainingRow> rows, int featureIndex) {
        Integer[] boxed = Arrays.stream(indices).boxed().toArray(Integer[]::new);
        Arrays.sort(boxed, Comparator.comparingDouble(index -> rows.get(index).features()[featureIndex]));
        for (int position = 0; position < boxed.length; position++) {
            indices[position] = boxed[position];
        }
    }

    private double[] distinctSortedValues(double[] values) {
        if (values.length == 0) {
            return new double[0];
        }

        double[] distinct = new double[values.length];
        int size = 0;
        double previous = Double.NaN;
        boolean first = true;
        for (double value : values) {
            if (first || Double.compare(value, previous) != 0) {
                distinct[size++] = value;
                previous = value;
                first = false;
            }
        }
        return Arrays.copyOf(distinct, size);
    }

    private double[] candidateThresholds(double[] uniqueValues, int maxThresholds) {
        if (uniqueValues.length <= 1) {
            return new double[0];
        }
        if (uniqueValues.length <= maxThresholds + 1) {
            double[] thresholds = new double[uniqueValues.length - 1];
            for (int index = 0; index < uniqueValues.length - 1; index++) {
                thresholds[index] = (uniqueValues[index] + uniqueValues[index + 1]) / 2.0;
            }
            return thresholds;
        }

        double[] thresholds = new double[maxThresholds];
        int size = 0;
        double step = (uniqueValues.length - 1.0) / (maxThresholds + 1.0);
        for (int candidateIndex = 1; candidateIndex <= maxThresholds; candidateIndex++) {
            int leftIndex = Math.min((int) (candidateIndex * step), uniqueValues.length - 2);
            int rightIndex = leftIndex + 1;
            double threshold = (uniqueValues[leftIndex] + uniqueValues[rightIndex]) / 2.0;
            if (size == 0 || Double.compare(threshold, thresholds[size - 1]) != 0) {
                thresholds[size++] = threshold;
            }
        }
        return Arrays.copyOf(thresholds, size);
    }

    private int upperBound(double[] values, double threshold) {
        int low = 0;
        int high = values.length;
        while (low < high) {
            int middle = (low + high) >>> 1;
            if (values[middle] <= threshold) {
                low = middle + 1;
            } else {
                high = middle;
            }
        }
        return low;
    }

    private double squaredError(int[] sortedIndices, int from, int to, double[] targets) {
        if (to <= from) {
            return 0.0;
        }

        double sum = 0.0;
        for (int position = from; position < to; position++) {
            sum += targets[sortedIndices[position]];
        }
        double average = sum / (to - from);

        double error = 0.0;
        for (int position = from; position < to; position++) {
            double delta = targets[sortedIndices[position]] - average;
            error += delta * delta;
        }
        return error;
    }

    private double[] constantPrediction(int size, double value) {
        double[] predictions = new double[size];
        Arrays.fill(predictions, value);
        return predictions;
    }

    private static Map<String, Object> treeInfo(double shrinkage, Map<String, Object> treeStructure) {
        Map<String, Object> tree = new LinkedHashMap<>();
        tree.put("shrinkage", shrinkage);
        tree.put("tree_structure", treeStructure);
        return tree;
    }

    private static Map<String, Object> leafNode(double value) {
        Map<String, Object> leaf = new LinkedHashMap<>();
        leaf.put("leaf_value", value);
        return leaf;
    }

    private double mean(double[] values) {
        if (values.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (double value : values) {
            sum += value;
        }
        return sum / values.length;
    }

    private double mean(int[] indices, double[] values) {
        if (indices.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int index : indices) {
            sum += values[index];
        }
        return sum / indices.length;
    }

    private double rmse(double[] actual, double[] predicted) {
        if (actual.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int index = 0; index < actual.length; index++) {
            double delta = actual[index] - predicted[index];
            sum += delta * delta;
        }
        return Math.sqrt(sum / actual.length);
    }

    private double mae(double[] actual, double[] predicted) {
        if (actual.length == 0) {
            return 0.0;
        }

        double sum = 0.0;
        for (int index = 0; index < actual.length; index++) {
            sum += Math.abs(actual[index] - predicted[index]);
        }
        return sum / actual.length;
    }

    private sealed interface TreeNode permits Leaf, Split {
        double predict(double[] features);

        Map<String, Object> toArtifactMap();
    }

    private record Leaf(double value) implements TreeNode {
        @Override
        public double predict(double[] features) {
            return value;
        }

        @Override
        public Map<String, Object> toArtifactMap() {
            return leafNode(value);
        }
    }

    private record Split(int featureIndex, double threshold, TreeNode leftChild, TreeNode rightChild) implements TreeNode {
        @Override
        public double predict(double[] features) {
            return features[featureIndex] <= threshold
                    ? leftChild.predict(features)
                    : rightChild.predict(features);
        }

        @Override
        public Map<String, Object> toArtifactMap() {
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("split_feature", featureIndex);
            node.put("threshold", threshold);
            node.put("decision_type", "<=");
            node.put("default_left", true);
            node.put("left_child", leftChild.toArtifactMap());
            node.put("right_child", rightChild.toArtifactMap());
            return node;
        }
    }

    private record SplitCandidate(int featureIndex, double threshold, double loss, int[] leftIndices, int[] rightIndices) {
    }
}
