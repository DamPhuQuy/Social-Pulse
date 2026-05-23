"""LightGBM trainer and evaluation helpers for feed ranking."""
from __future__ import annotations

import math
import os
from typing import Any

import numpy as np
from sklearn.metrics import ndcg_score, r2_score

from ai_pipeline.shared.schema import RankingFeatureSchema
from .arguments import TrainingArguments
from .types import Metrics, TrainedRankingModel, TrainingHistoryPoint, TrainingRow


class LightGbmRankingTrainer:
    def train(
        self,
        arguments: TrainingArguments,
        train_rows: list[TrainingRow],
        validation_rows: list[TrainingRow],
    ) -> TrainedRankingModel:
        x_train = _feature_matrix(train_rows, "train")
        y_train = _labels(train_rows, "train")
        x_val = _feature_matrix(validation_rows, "validation")
        y_val = _labels(validation_rows, "validation")

        try:
            import lightgbm as lgb
        except ImportError as exc:
            raise RuntimeError("lightgbm is required for training. Run `uv sync` first.") from exc

        feature_names = list(RankingFeatureSchema.FEATURE_ORDER)
        params = self._build_params(arguments)
        dtrain = lgb.Dataset(x_train, label=y_train, feature_name=feature_names, free_raw_data=False)
        dval = lgb.Dataset(x_val, label=y_val, feature_name=feature_names, free_raw_data=False, reference=dtrain)

        eval_log: dict[str, dict[str, list[float]]] = {}
        callbacks = [
            lgb.early_stopping(stopping_rounds=arguments.early_stopping_rounds, verbose=True),
            lgb.log_evaluation(period=25),
            lgb.record_evaluation(eval_log),
        ]

        active_device = arguments.device
        try:
            booster = lgb.train(
                params,
                dtrain,
                num_boost_round=arguments.n_estimators,
                valid_sets=[dtrain, dval],
                valid_names=["train", "valid"],
                callbacks=callbacks,
            )
        except Exception as exc:
            if not arguments.allow_cpu_fallback or arguments.device != "cuda":
                raise
            print(f"GPU training unavailable ({exc}); retrying with LightGBM CPU.")
            active_device = "cpu"
            params["device_type"] = "cpu"
            eval_log.clear()
            booster = lgb.train(
                params,
                dtrain,
                num_boost_round=arguments.n_estimators,
                valid_sets=[dtrain, dval],
                valid_names=["train", "valid"],
                callbacks=callbacks,
            )

        best_iteration = booster.best_iteration or arguments.n_estimators
        train_pred = booster.predict(x_train, num_iteration=best_iteration)
        val_pred = booster.predict(x_val, num_iteration=best_iteration)
        feature_importances = dict(
            zip(
                feature_names,
                booster.feature_importance(importance_type="gain").tolist(),
            )
        )

        print(f"Training backend: lightgbm ({'GPU' if active_device == 'cuda' else 'CPU'})")
        print(f"Best iteration: {best_iteration}")
        print("\nFeature importance (top 10):")
        for name, importance in sorted(feature_importances.items(), key=lambda item: item[1], reverse=True)[:10]:
            print(f"  {name}: {importance:.4f}")

        metrics = Metrics(
            train_rmse=_rmse(y_train, train_pred),
            validation_rmse=_rmse(y_val, val_pred),
            test_rmse=0.0,
            train_mae=_mae(y_train, train_pred),
            validation_mae=_mae(y_val, val_pred),
            test_mae=0.0,
            train_ndcg_k=_ndcg(train_rows, train_pred, k=10),
            validation_ndcg_k=_ndcg(validation_rows, val_pred, k=10),
            test_ndcg_k=0.0,
            train_r2=_r2(y_train, train_pred),
            validation_r2=_r2(y_val, val_pred),
            test_r2=0.0,
        )

        return TrainedRankingModel(
            backend="lightgbm",
            runtime_model=booster,
            metrics=metrics,
            history=_build_lgb_history(eval_log),
            feature_importances=feature_importances,
            best_iteration=best_iteration,
        )

    @staticmethod
    def _build_params(arguments: TrainingArguments) -> dict[str, Any]:
        n_jobs = arguments.n_jobs if arguments.n_jobs > 0 else max(1, os.cpu_count() or 1)
        return {
            "objective": "regression",
            "metric": ["rmse", "mae"],
            "max_depth": arguments.max_depth,
            "num_leaves": max(2, 2 ** arguments.max_depth - 1),
            "min_child_samples": arguments.min_samples_leaf,
            "min_child_weight": arguments.min_child_weight,
            "learning_rate": arguments.learning_rate,
            "subsample": arguments.subsample,
            "subsample_freq": 1,
            "colsample_bytree": arguments.colsample_bytree,
            "reg_lambda": arguments.reg_lambda,
            "reg_alpha": arguments.reg_alpha,
            "max_bin": arguments.max_bin,
            "random_state": arguments.seed,
            "n_jobs": n_jobs,
            "device_type": "gpu" if arguments.device == "cuda" else "cpu",
            "verbose": -1,
        }

    def evaluate(self, runtime_model, rows: list[TrainingRow]) -> Metrics:
        if not rows:
            return Metrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        x = _feature_matrix(rows, "test")
        y = _labels(rows, "test")
        predictions = _predict(runtime_model, x)
        return Metrics(
            train_rmse=0.0,
            validation_rmse=0.0,
            test_rmse=_rmse(y, predictions),
            train_mae=0.0,
            validation_mae=0.0,
            test_mae=_mae(y, predictions),
            train_ndcg_k=0.0,
            validation_ndcg_k=0.0,
            test_ndcg_k=_ndcg(rows, predictions, k=10),
            train_r2=0.0,
            validation_r2=0.0,
            test_r2=_r2(y, predictions),
        )

    def evaluate_baseline(self, rows: list[TrainingRow], prediction_value: float) -> Metrics:
        if not rows:
            return Metrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        y = _labels(rows, "baseline")
        predictions = np.full(len(rows), prediction_value, dtype=np.float32)
        return Metrics(
            train_rmse=0.0,
            validation_rmse=0.0,
            test_rmse=_rmse(y, predictions),
            train_mae=0.0,
            validation_mae=0.0,
            test_mae=_mae(y, predictions),
            train_ndcg_k=0.0,
            validation_ndcg_k=0.0,
            test_ndcg_k=_ndcg(rows, predictions, k=10),
            train_r2=0.0,
            validation_r2=0.0,
            test_r2=_r2(y, predictions),
        )

    def evaluation_diagnostics(self, runtime_model, rows: list[TrainingRow]) -> dict[str, float | int | dict[str, float]]:
        if not rows:
            return {}
        x = _feature_matrix(rows, "diagnostics")
        y = _labels(rows, "diagnostics")
        predictions = _predict(runtime_model, x)
        return {
            "label_stats": _label_stats(y),
            "prediction_stats": _label_stats(np.array(predictions, dtype=np.float32)),
            "ranking": _ranking_diagnostics(rows, predictions),
        }


def _feature_matrix(rows: list[TrainingRow], name: str) -> np.ndarray:
    x = np.array([row.features for row in rows], dtype=np.float32)
    y = np.array([row.label for row in rows], dtype=np.float32)
    _validate_data(x, y, name)
    return x


def _labels(rows: list[TrainingRow], name: str) -> np.ndarray:
    x = np.array([row.features for row in rows], dtype=np.float32)
    y = np.array([row.label for row in rows], dtype=np.float32)
    _validate_data(x, y, name)
    return y


def _validate_data(x: np.ndarray, y: np.ndarray, name: str) -> None:
    if len(x) == 0:
        raise ValueError(f"{name} set is empty")
    if x.shape[1] != len(RankingFeatureSchema.FEATURE_ORDER):
        raise ValueError(
            f"{name} set has {x.shape[1]} features, expected {len(RankingFeatureSchema.FEATURE_ORDER)}"
        )
    if np.any(np.isnan(x)):
        nan_columns = np.where(np.any(np.isnan(x), axis=0))[0]
        raise ValueError(f"{name} set has NaN values in feature columns: {nan_columns.tolist()}")
    if np.any(np.isinf(x)):
        raise ValueError(f"{name} set has infinite values")
    if np.any(np.isnan(y)) or np.any(np.isinf(y)):
        raise ValueError(f"{name} labels contain NaN or infinite values")


def _predict(runtime_model, x: np.ndarray) -> np.ndarray:
    best_iteration = getattr(runtime_model, "best_iteration", None)
    if best_iteration:
        return runtime_model.predict(x, num_iteration=best_iteration)
    return runtime_model.predict(x)


def _rmse(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(math.sqrt(((actual - predicted) ** 2).mean()))


def _mae(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) == 0:
        return 0.0
    return float(np.abs(actual - predicted).mean())


def _r2(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) <= 1 or float(np.var(actual)) == 0.0:
        return 0.0
    return float(r2_score(actual, predicted))


def _ndcg(rows: list[TrainingRow], predictions: np.ndarray, k: int = 10) -> float:
    from collections import defaultdict

    groups: dict[str, list[int]] = defaultdict(list)
    for idx, row in enumerate(rows):
        groups[row.viewer_id].append(idx)

    ndcg_sum = 0.0
    count = 0
    for indices in groups.values():
        if len(indices) < 2:
            continue
        relevances = np.array([rows[i].label for i in indices], dtype=np.float32)
        if float(relevances.max()) == float(relevances.min()):
            continue
        scores = predictions[indices]
        ndcg_sum += float(
            ndcg_score(
                relevances.reshape(1, -1),
                np.array(scores, dtype=np.float32).reshape(1, -1),
                k=min(k, len(indices)),
                ignore_ties=False,
            )
        )
        count += 1
    return ndcg_sum / count if count > 0 else 0.0


def _label_stats(values: np.ndarray) -> dict[str, float]:
    if len(values) == 0:
        return {"mean": 0.0, "std": 0.0, "min": 0.0, "max": 0.0, "zero_ratio": 0.0}
    return {
        "mean": float(values.mean()),
        "std": float(values.std()),
        "min": float(values.min()),
        "max": float(values.max()),
        "zero_ratio": float((values == 0).sum() / len(values)),
    }


def _ranking_diagnostics(rows: list[TrainingRow], predictions: np.ndarray) -> dict[str, float | int]:
    from collections import defaultdict

    groups: dict[str, list[int]] = defaultdict(list)
    for idx, row in enumerate(rows):
        groups[row.viewer_id].append(idx)

    ranked_groups = 0
    comparable_pairs = 0
    correct_pairs = 0.0
    for indices in groups.values():
        if len(indices) < 2:
            continue
        labels = np.array([rows[i].label for i in indices], dtype=np.float32)
        if float(labels.max()) == float(labels.min()):
            continue
        ranked_groups += 1
        scores = predictions[indices]
        for left in range(len(indices)):
            for right in range(left + 1, len(indices)):
                label_diff = labels[left] - labels[right]
                if label_diff == 0:
                    continue
                comparable_pairs += 1
                score_diff = scores[left] - scores[right]
                if score_diff == 0:
                    correct_pairs += 0.5
                elif (score_diff > 0) == (label_diff > 0):
                    correct_pairs += 1.0

    return {
        "group_count": len(groups),
        "ranked_group_count": ranked_groups,
        "comparable_pair_count": comparable_pairs,
        "pairwise_accuracy": float(correct_pairs / comparable_pairs) if comparable_pairs > 0 else 0.0,
    }


def _build_lgb_history(eval_log: dict[str, dict[str, list[float]]]) -> list[TrainingHistoryPoint]:
    train_rmse = eval_log.get("train", {}).get("rmse", [])
    valid_rmse = eval_log.get("valid", {}).get("rmse", [])
    train_mae = eval_log.get("train", {}).get("mae", eval_log.get("train", {}).get("l1", []))
    valid_mae = eval_log.get("valid", {}).get("mae", eval_log.get("valid", {}).get("l1", []))
    count = max(len(train_rmse), len(valid_rmse), len(train_mae), len(valid_mae))

    return [
        TrainingHistoryPoint(
            iteration=idx + 1,
            train_rmse=train_rmse[idx] if idx < len(train_rmse) else None,
            validation_rmse=valid_rmse[idx] if idx < len(valid_rmse) else None,
            train_mae=train_mae[idx] if idx < len(train_mae) else None,
            validation_mae=valid_mae[idx] if idx < len(valid_mae) else None,
        )
        for idx in range(count)
    ]
