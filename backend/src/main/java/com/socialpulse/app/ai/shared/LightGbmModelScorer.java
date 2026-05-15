package com.socialpulse.app.ai.shared;

import java.util.Map;

public class LightGbmModelScorer {
    private final LightGbmModel model;

    public LightGbmModelScorer(LightGbmModel model) {
        if (model == null || model.getTreeInfo() == null || model.getTreeInfo().isEmpty()) {
            throw new IllegalArgumentException("LightGBM model must contain at least one tree");
        }
        this.model = model;
    }

    public double score(Map<String, Double> features) {
        double total = 0.0;

        for (LightGbmModel.TreeInfo treeInfo : model.getTreeInfo()) {
            double treeScore = scoreNode(treeInfo.getTreeStructure(), features);
            total += treeScore * safeDouble(treeInfo.getShrinkage(), 1.0);
        }

        if (Boolean.TRUE.equals(model.getAverageOutput())) {
            total /= model.getTreeInfo().size();
        }

        return total;
    }

    private double scoreNode(LightGbmModel.TreeNode node, Map<String, Double> features) {
        if (node == null) {
            throw new IllegalArgumentException("Encountered null tree node while scoring LightGBM model");
        }

        if (node.isLeaf()) {
            return safeDouble(node.getLeafValue(), 0.0);
        }

        if (node.getSplitFeature() == null) {
            throw new IllegalArgumentException("Non-leaf LightGBM node is missing split_feature");
        }

        String featureName = model.getFeatureName(node.getSplitFeature());
        ResolvedFeature resolvedFeature = resolveFeatureValue(featureName, features);
        boolean goLeft = shouldGoLeft(node, resolvedFeature);
        return goLeft
                ? scoreNode(node.getLeftChild(), features)
                : scoreNode(node.getRightChild(), features);
    }

    private ResolvedFeature resolveFeatureValue(String featureName, Map<String, Double> features) {
        if (!features.containsKey(featureName)) {
            return new ResolvedFeature(LightGbmFeatureSchema.DEFAULT_NUMERIC_VALUE, false);
        }

        Double value = features.get(featureName);
        if (value == null || Double.isNaN(value)) {
            return new ResolvedFeature(Double.NaN, true);
        }

        return new ResolvedFeature(value, false);
    }

    private boolean shouldGoLeft(LightGbmModel.TreeNode node, ResolvedFeature resolvedFeature) {
        if (resolvedFeature.missing()) {
            return Boolean.TRUE.equals(node.getDefaultLeft());
        }

        double threshold = safeDouble(node.getThreshold(), 0.0);
        String decisionType = node.getDecisionType();
        double featureValue = resolvedFeature.value();

        if (decisionType == null || decisionType.isBlank() || "<=".equals(decisionType)) {
            return featureValue <= threshold;
        }

        if ("<".equals(decisionType)) {
            return featureValue < threshold;
        }

        if (">".equals(decisionType)) {
            return featureValue > threshold;
        }

        if (">=".equals(decisionType)) {
            return featureValue >= threshold;
        }

        if ("==".equals(decisionType)) {
            return Double.compare(featureValue, threshold) == 0;
        }

        throw new IllegalArgumentException("Unsupported LightGBM decision type: " + decisionType);
    }

    private double safeDouble(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    private record ResolvedFeature(double value, boolean missing) {
    }
}
