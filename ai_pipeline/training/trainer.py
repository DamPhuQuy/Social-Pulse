"""Trainer using scikit-learn GradientBoostingRegressor."""
from __future__ import annotations

import math
from typing import Any

import numpy as np
from sklearn.ensemble import GradientBoostingRegressor

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from .arguments import TrainingArguments
from .types import GradientBoostedModel, Metrics, TrainingRow


class GradientBoostedTreeTrainer:

    def train(
        self, arguments: TrainingArguments, train_rows: list[TrainingRow], validation_rows: list[TrainingRow]
    ) -> GradientBoostedModel:
        X_train = np.array([r.features for r in train_rows])
        y_train = np.array([r.label for r in train_rows])
        X_val = np.array([r.features for r in validation_rows])
        y_val = np.array([r.label for r in validation_rows])

        # Validate data
        _validate_data(X_train, y_train, "train")
        _validate_data(X_val, y_val, "validation")

        model = GradientBoostingRegressor(
            n_estimators=arguments.n_estimators,
            max_depth=arguments.max_depth,
            min_samples_leaf=arguments.min_samples_leaf,
            learning_rate=arguments.learning_rate,
            subsample=0.8,
            random_state=arguments.seed,
        )

        # Fit with early stopping via staged_predict
        best_n_estimators = arguments.n_estimators
        best_val_ndcg = -1.0
        rounds_no_improve = 0

        # Train full model first, then find best iteration via staged_predict
        model.fit(X_train, y_train)

        # Evaluate at each stage to find best iteration by NDCG
        best_train_preds = None
        best_val_preds = None

        for i, (train_pred, val_pred) in enumerate(
            zip(model.staged_predict(X_train), model.staged_predict(X_val))
        ):
            val_ndcg = _ndcg(validation_rows, val_pred, k=10)

            if (i + 1) % 10 == 0 or i == 0:
                train_rmse = _rmse(y_train, train_pred)
                val_rmse = _rmse(y_val, val_pred)
                print(f"[iter {i + 1:>3}] train_rmse={train_rmse:.6f}  val_rmse={val_rmse:.6f}  val_ndcg={val_ndcg:.6f}")

            if val_ndcg > best_val_ndcg:
                best_val_ndcg = val_ndcg
                best_n_estimators = i + 1
                best_train_preds = train_pred.copy()
                best_val_preds = val_pred.copy()
                rounds_no_improve = 0
            else:
                rounds_no_improve += 1
                if rounds_no_improve >= arguments.early_stopping_rounds:
                    print(f"Early stopping at iteration {i + 1}, best val_ndcg@10={best_val_ndcg:.6f} at iter {best_n_estimators}")
                    break

        # Trim model to best iteration
        model.n_estimators_ = best_n_estimators
        model.estimators_ = model.estimators_[:best_n_estimators]

        if best_train_preds is None:
            best_train_preds = model.predict(X_train)
            best_val_preds = model.predict(X_val)

        # Feature importance
        feature_names = list(LightGbmFeatureSchema.FEATURE_ORDER)
        importances = model.feature_importances_
        importance_ranking = sorted(
            zip(feature_names, importances), key=lambda x: x[1], reverse=True
        )
        print("\nFeature importance (top 10):")
        for name, imp in importance_ranking[:10]:
            print(f"  {name}: {imp:.4f}")

        metrics = Metrics(
            train_rmse=_rmse(y_train, best_train_preds),
            validation_rmse=_rmse(y_val, best_val_preds),
            train_mae=_mae(y_train, best_train_preds),
            validation_mae=_mae(y_val, best_val_preds),
            train_ndcg_k=_ndcg(train_rows, best_train_preds, k=10),
            validation_ndcg_k=best_val_ndcg,
        )

        model_dump = _export_model(model, feature_names, importances)
        return GradientBoostedModel(model_dump, metrics)


def _export_model(model: GradientBoostingRegressor, feature_names: list[str], importances: np.ndarray) -> dict[str, Any]:
    """Export sklearn model to the same JSON format used by the scorer."""
    tree_info: list[dict[str, Any]] = []

    # Bias tree (init prediction)
    init_value = float(model.init_.constant_[0][0]) if hasattr(model.init_, 'constant_') else 0.0
    tree_info.append({"shrinkage": 1.0, "tree_structure": {"leaf_value": init_value}})

    # Each boosting iteration
    for i in range(model.n_estimators_):
        tree = model.estimators_[i, 0].tree_
        tree_info.append({
            "shrinkage": model.learning_rate,
            "tree_structure": _tree_to_dict(tree, 0),
        })

    return {
        "objective": "regression",
        "average_output": False,
        "feature_names": feature_names,
        "tree_info": tree_info,
        "feature_importances": dict(zip(feature_names, importances.tolist())),
    }


def _tree_to_dict(tree, node_id: int) -> dict[str, Any]:
    """Recursively convert sklearn tree to our JSON format."""
    if tree.children_left[node_id] == -1:  # leaf
        return {"leaf_value": float(tree.value[node_id][0, 0])}
    return {
        "split_feature": int(tree.feature[node_id]),
        "threshold": float(tree.threshold[node_id]),
        "decision_type": "<=",
        "default_left": True,
        "left_child": _tree_to_dict(tree, tree.children_left[node_id]),
        "right_child": _tree_to_dict(tree, tree.children_right[node_id]),
    }


def _validate_data(X: np.ndarray, y: np.ndarray, name: str) -> None:
    if len(X) == 0:
        raise ValueError(f"{name} set is empty")
    if np.any(np.isnan(X)):
        nan_cols = np.where(np.any(np.isnan(X), axis=0))[0]
        raise ValueError(f"{name} set has NaN values in feature columns: {nan_cols.tolist()}")
    if np.any(np.isinf(X)):
        raise ValueError(f"{name} set has infinite values")
    if np.any(np.isnan(y)) or np.any(np.isinf(y)):
        raise ValueError(f"{name} labels contain NaN or infinite values")


def _rmse(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(math.sqrt(((actual - predicted) ** 2).mean()))


def _mae(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(np.abs(actual - predicted).mean())


def _ndcg(rows: list[TrainingRow], predictions: np.ndarray, k: int = 10) -> float:
    """Compute NDCG@k grouped by post_id (each post's viewers form a group)."""
    from collections import defaultdict
    groups: dict[str, list[int]] = defaultdict(list)
    for i, row in enumerate(rows):
        groups[row.post_id].append(i)
    if not groups:
        return 0.0
    ndcg_sum = 0.0
    count = 0
    for indices in groups.values():
        if len(indices) < 2:
            continue
        relevances = np.array([rows[i].label for i in indices])
        scores = predictions[indices]
        ranked = np.argsort(-scores)[:k]
        dcg = float(np.sum((2.0 ** relevances[ranked] - 1.0) / np.log2(np.arange(len(ranked)) + 2.0)))
        ideal_order = np.argsort(-relevances)[:k]
        idcg = float(np.sum((2.0 ** relevances[ideal_order] - 1.0) / np.log2(np.arange(len(ideal_order)) + 2.0)))
        ndcg_sum += dcg / idcg if idcg > 0 else 1.0  # perfect score if all same relevance
        count += 1
    return ndcg_sum / count if count > 0 else 0.0
