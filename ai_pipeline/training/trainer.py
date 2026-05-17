"""Custom gradient boosted tree trainer - pure Python GBDT implementation."""
from __future__ import annotations

import math
from typing import Any

import numpy as np

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from .arguments import TrainingArguments
from .types import GradientBoostedModel, Metrics, TrainingRow


class GradientBoostedTreeTrainer:

    def train(
        self, arguments: TrainingArguments, train_rows: list[TrainingRow], validation_rows: list[TrainingRow]
    ) -> GradientBoostedModel:
        train_targets = np.array([r.label for r in train_rows])
        val_targets = np.array([r.label for r in validation_rows])

        bias = float(train_targets.mean()) if len(train_targets) > 0 else 0.0
        tree_info: list[dict[str, Any]] = [{"shrinkage": 1.0, "tree_structure": {"leaf_value": bias}}]

        train_preds = np.full(len(train_rows), bias)
        val_preds = np.full(len(validation_rows), bias)

        train_features = np.array([r.features for r in train_rows])
        val_features = np.array([r.features for r in validation_rows]) if validation_rows else np.empty((0, len(LightGbmFeatureSchema.FEATURE_ORDER)))

        for _ in range(arguments.n_estimators):
            residuals = train_targets - train_preds
            indices = np.arange(len(train_rows))

            tree = self._build_tree(
                train_features, residuals, indices, 0,
                arguments.max_depth, arguments.min_samples_leaf, arguments.max_thresholds,
            )
            tree_info.append({"shrinkage": arguments.learning_rate, "tree_structure": tree.to_dict()})

            train_preds += arguments.learning_rate * tree.predict_batch(train_features)
            if len(val_features) > 0:
                val_preds += arguments.learning_rate * tree.predict_batch(val_features)

        metrics = Metrics(
            train_rmse=_rmse(train_targets, train_preds),
            validation_rmse=_rmse(val_targets, val_preds),
            train_mae=_mae(train_targets, train_preds),
            validation_mae=_mae(val_targets, val_preds),
        )
        model_dump = {
            "objective": "regression",
            "average_output": False,
            "feature_names": list(LightGbmFeatureSchema.FEATURE_ORDER),
            "tree_info": tree_info,
        }
        return GradientBoostedModel(model_dump, metrics)

    def _build_tree(
        self, features: np.ndarray, targets: np.ndarray, indices: np.ndarray,
        depth: int, max_depth: int, min_samples_leaf: int, max_thresholds: int,
    ) -> _TreeNode:
        prediction = float(targets[indices].mean()) if len(indices) > 0 else 0.0
        if depth >= max_depth or len(indices) < min_samples_leaf * 2:
            return _Leaf(prediction)

        best = self._find_best_split(features, targets, indices, min_samples_leaf, max_thresholds)
        if best is None:
            return _Leaf(prediction)

        return _Split(
            best[0], best[1],
            self._build_tree(features, targets, best[2], depth + 1, max_depth, min_samples_leaf, max_thresholds),
            self._build_tree(features, targets, best[3], depth + 1, max_depth, min_samples_leaf, max_thresholds),
        )

    def _find_best_split(
        self, features: np.ndarray, targets: np.ndarray, indices: np.ndarray,
        min_samples_leaf: int, max_thresholds: int,
    ) -> tuple[int, float, np.ndarray, np.ndarray] | None:
        best = None
        best_loss = float("inf")
        n_features = features.shape[1]

        for fi in range(n_features):
            col = features[indices, fi]
            sorted_order = np.argsort(col)
            sorted_indices = indices[sorted_order]
            sorted_vals = col[sorted_order]

            unique_vals = np.unique(sorted_vals)
            if len(unique_vals) <= 1:
                continue

            thresholds = _candidate_thresholds(unique_vals, max_thresholds)
            for threshold in thresholds:
                split_pos = int(np.searchsorted(sorted_vals, threshold, side="right"))
                if split_pos < min_samples_leaf or len(sorted_indices) - split_pos < min_samples_leaf:
                    continue

                left_idx = sorted_indices[:split_pos]
                right_idx = sorted_indices[split_pos:]
                loss = _squared_error(targets, left_idx) + _squared_error(targets, right_idx)

                if loss < best_loss:
                    best_loss = loss
                    best = (fi, threshold, left_idx, right_idx)

        return best


# ─── Tree Node Types ──────────────────────────────────────────────────────────

class _TreeNode:
    def predict(self, features: np.ndarray) -> float:
        raise NotImplementedError

    def predict_batch(self, features: np.ndarray) -> np.ndarray:
        return np.array([self.predict(features[i]) for i in range(len(features))])

    def to_dict(self) -> dict[str, Any]:
        raise NotImplementedError


class _Leaf(_TreeNode):
    def __init__(self, value: float):
        self.value = value

    def predict(self, features: np.ndarray) -> float:
        return self.value

    def predict_batch(self, features: np.ndarray) -> np.ndarray:
        return np.full(len(features), self.value)

    def to_dict(self) -> dict[str, Any]:
        return {"leaf_value": self.value}


class _Split(_TreeNode):
    def __init__(self, feature_index: int, threshold: float, left: _TreeNode, right: _TreeNode):
        self.feature_index = feature_index
        self.threshold = threshold
        self.left = left
        self.right = right

    def predict(self, features: np.ndarray) -> float:
        return self.left.predict(features) if features[self.feature_index] <= self.threshold else self.right.predict(features)

    def predict_batch(self, features: np.ndarray) -> np.ndarray:
        mask = features[:, self.feature_index] <= self.threshold
        result = np.empty(len(features))
        if mask.any():
            result[mask] = self.left.predict_batch(features[mask])
        if (~mask).any():
            result[~mask] = self.right.predict_batch(features[~mask])
        return result

    def to_dict(self) -> dict[str, Any]:
        return {
            "split_feature": self.feature_index,
            "threshold": self.threshold,
            "decision_type": "<=",
            "default_left": True,
            "left_child": self.left.to_dict(),
            "right_child": self.right.to_dict(),
        }


# ─── Helpers ──────────────────────────────────────────────────────────────────

def _candidate_thresholds(unique_vals: np.ndarray, max_thresholds: int) -> np.ndarray:
    if len(unique_vals) <= 1:
        return np.array([])
    if len(unique_vals) <= max_thresholds + 1:
        return (unique_vals[:-1] + unique_vals[1:]) / 2.0
    step = (len(unique_vals) - 1) / (max_thresholds + 1)
    thresholds = []
    for i in range(1, max_thresholds + 1):
        left_idx = min(int(i * step), len(unique_vals) - 2)
        t = (unique_vals[left_idx] + unique_vals[left_idx + 1]) / 2.0
        if not thresholds or t != thresholds[-1]:
            thresholds.append(t)
    return np.array(thresholds)


def _squared_error(targets: np.ndarray, indices: np.ndarray) -> float:
    if len(indices) == 0:
        return 0.0
    subset = targets[indices]
    mean = subset.mean()
    return float(((subset - mean) ** 2).sum())


def _rmse(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(math.sqrt(((actual - predicted) ** 2).mean()))


def _mae(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(np.abs(actual - predicted).mean())
