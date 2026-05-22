"""Trainer implementations for ranking model training."""
from __future__ import annotations

import math
import os
from typing import Any

import numpy as np
from sklearn.ensemble import GradientBoostingRegressor
from sklearn.metrics import ndcg_score, r2_score

from ai_pipeline.shared.schema import RankingFeatureSchema
from .arguments import TrainingArguments
from .types import Metrics, TrainedRankingModel, TrainingHistoryPoint, TrainingRow


class GradientBoostedTreeTrainer:
    def train(
        self,
        arguments: TrainingArguments,
        train_rows: list[TrainingRow],
        validation_rows: list[TrainingRow],
    ) -> TrainedRankingModel:
        x_train = np.array([row.features for row in train_rows], dtype=np.float32)
        y_train = np.array([row.label for row in train_rows], dtype=np.float32)
        x_val = np.array([row.features for row in validation_rows], dtype=np.float32)
        y_val = np.array([row.label for row in validation_rows], dtype=np.float32)

        _validate_data(x_train, y_train, "train")
        _validate_data(x_val, y_val, "validation")

        if arguments.trainer_backend == "lightgbm":
            return self._train_lightgbm(arguments, train_rows, validation_rows, x_train, y_train, x_val, y_val)
        return self._train_sklearn(arguments, train_rows, validation_rows, x_train, y_train, x_val, y_val)

    @staticmethod
    def _resolve_n_jobs(arguments: TrainingArguments) -> int:
        return arguments.n_jobs if arguments.n_jobs > 0 else max(1, os.cpu_count() or 1)

    def _train_sklearn(
        self,
        arguments: TrainingArguments,
        train_rows: list[TrainingRow],
        validation_rows: list[TrainingRow],
        x_train: np.ndarray,
        y_train: np.ndarray,
        x_val: np.ndarray,
        y_val: np.ndarray,
    ) -> TrainedRankingModel:
        model = GradientBoostingRegressor(
            n_estimators=arguments.n_estimators,
            max_depth=arguments.max_depth,
            min_samples_leaf=arguments.min_samples_leaf,
            learning_rate=arguments.learning_rate,
            subsample=arguments.subsample,
            random_state=arguments.seed,
        )

        best_n_estimators = arguments.n_estimators
        best_val_ndcg = -1.0
        rounds_no_improve = 0
        best_train_preds = None
        best_val_preds = None
        history: list[TrainingHistoryPoint] = []

        model.fit(x_train, y_train)

        for idx, (train_pred, val_pred) in enumerate(zip(model.staged_predict(x_train), model.staged_predict(x_val))):
            train_rmse = _rmse(y_train, train_pred)
            validation_rmse = _rmse(y_val, val_pred)
            train_mae = _mae(y_train, train_pred)
            validation_mae = _mae(y_val, val_pred)
            validation_ndcg = _ndcg(validation_rows, val_pred, k=10)

            history.append(
                TrainingHistoryPoint(
                    iteration=idx + 1,
                    train_rmse=train_rmse,
                    validation_rmse=validation_rmse,
                    train_mae=train_mae,
                    validation_mae=validation_mae,
                )
            )

            if (idx + 1) % 10 == 0 or idx == 0:
                print(
                    f"[iter {idx + 1:>4}] "
                    f"train_rmse={train_rmse:.6f}  "
                    f"val_rmse={validation_rmse:.6f}  "
                    f"val_ndcg={validation_ndcg:.6f}"
                )

            if validation_ndcg > best_val_ndcg:
                best_val_ndcg = validation_ndcg
                best_n_estimators = idx + 1
                best_train_preds = train_pred.copy()
                best_val_preds = val_pred.copy()
                rounds_no_improve = 0
            else:
                rounds_no_improve += 1
                if rounds_no_improve >= arguments.early_stopping_rounds:
                    print(
                        f"Early stopping at iteration {idx + 1}, "
                        f"best val_ndcg@10={best_val_ndcg:.6f} at iter {best_n_estimators}"
                    )
                    break

        model.n_estimators_ = best_n_estimators
        model.estimators_ = model.estimators_[:best_n_estimators]

        if best_train_preds is None:
            best_train_preds = model.predict(x_train)
            best_val_preds = model.predict(x_val)

        feature_names = list(RankingFeatureSchema.FEATURE_ORDER)
        importances = model.feature_importances_
        feature_importances = dict(zip(feature_names, importances.tolist()))

        print("\nFeature importance (top 10):")
        for name, importance in sorted(feature_importances.items(), key=lambda item: item[1], reverse=True)[:10]:
            print(f"  {name}: {importance:.4f}")

        metrics = Metrics(
            train_rmse=_rmse(y_train, best_train_preds),
            validation_rmse=_rmse(y_val, best_val_preds),
            test_rmse=0.0,
            train_mae=_mae(y_train, best_train_preds),
            validation_mae=_mae(y_val, best_val_preds),
            test_mae=0.0,
            train_ndcg_k=_ndcg(train_rows, best_train_preds, k=10),
            validation_ndcg_k=best_val_ndcg,
            test_ndcg_k=0.0,
            train_r2=_r2(y_train, best_train_preds),
            validation_r2=_r2(y_val, best_val_preds),
            test_r2=0.0,
        )

        model_dump = _export_model(model, feature_names, importances)
        return TrainedRankingModel(
            backend="sklearn",
            runtime_model=model,
            model_dump=model_dump,
            metrics=metrics,
            history=history,
            feature_importances=feature_importances,
            best_iteration=best_n_estimators,
        )

    def _train_lightgbm(
        self,
        arguments: TrainingArguments,
        train_rows: list[TrainingRow],
        validation_rows: list[TrainingRow],
        x_train: np.ndarray,
        y_train: np.ndarray,
        x_val: np.ndarray,
        y_val: np.ndarray,
    ) -> TrainedRankingModel:
        try:
            import lightgbm as lgb
        except ImportError as exc:
            if arguments.allow_cpu_fallback:
                print("lightgbm is not installed; falling back to sklearn GradientBoostingRegressor.")
                fallback_args = TrainingArguments(**{**arguments.__dict__, "trainer_backend": "sklearn", "device": "cpu"})
                return self._train_sklearn(fallback_args, train_rows, validation_rows, x_train, y_train, x_val, y_val)
            raise RuntimeError("lightgbm is required. Install dependencies first.") from exc

        feature_names = list(RankingFeatureSchema.FEATURE_ORDER)
        n_jobs = self._resolve_n_jobs(arguments)

        # LightGBM maps "cuda" → "gpu"; CPU training uses the default.
        device_type = "gpu" if arguments.device == "cuda" else "cpu"

        params: dict[str, Any] = dict(
            objective="regression",
            metric=["rmse", "mae"],
            n_estimators=arguments.n_estimators,
            max_depth=arguments.max_depth,
            num_leaves=max(2, 2 ** arguments.max_depth - 1),
            min_child_samples=arguments.min_samples_leaf,
            min_child_weight=arguments.min_child_weight,
            learning_rate=arguments.learning_rate,
            subsample=arguments.subsample,
            subsample_freq=1,
            colsample_bytree=arguments.colsample_bytree,
            reg_lambda=arguments.reg_lambda,
            reg_alpha=arguments.reg_alpha,
            max_bin=arguments.max_bin,
            random_state=arguments.seed,
            n_jobs=n_jobs,
            device_type=device_type,
            verbose=-1,
        )

        dtrain = lgb.Dataset(x_train, label=y_train, feature_name=feature_names, free_raw_data=False)
        dval   = lgb.Dataset(x_val,   label=y_val,   feature_name=feature_names, free_raw_data=False, reference=dtrain)

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
            print(f"GPU training unavailable ({exc}); retrying on CPU.")
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
        val_pred   = booster.predict(x_val,   num_iteration=best_iteration)

        history = _build_lgb_history(eval_log)

        raw_importances = booster.feature_importance(importance_type="gain")
        feature_importances = dict(zip(feature_names, raw_importances.tolist()))

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
            model_dump=None,
            metrics=metrics,
            history=history,
            feature_importances=feature_importances,
            best_iteration=best_iteration,
        )

    def evaluate(
        self,
        backend: str,
        runtime_model,
        rows: list[TrainingRow],
    ) -> Metrics:
        if not rows:
            return Metrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)

        x = np.array([row.features for row in rows], dtype=np.float32)
        y = np.array([row.label for row in rows], dtype=np.float32)
        _validate_data(x, y, "test")

        if backend == "lightgbm":
            predictions = runtime_model.predict(x)
        else:
            predictions = runtime_model.predict(x)

        score_rmse = _rmse(y, predictions)
        score_mae = _mae(y, predictions)
        score_ndcg = _ndcg(rows, predictions, k=10)
        score_r2 = _r2(y, predictions)
        return Metrics(
            train_rmse=0.0,
            validation_rmse=0.0,
            test_rmse=score_rmse,
            train_mae=0.0,
            validation_mae=0.0,
            test_mae=score_mae,
            train_ndcg_k=0.0,
            validation_ndcg_k=0.0,
            test_ndcg_k=score_ndcg,
            train_r2=0.0,
            validation_r2=0.0,
            test_r2=score_r2,
        )

    def evaluate_baseline(self, rows: list[TrainingRow], prediction_value: float) -> Metrics:
        if not rows:
            return Metrics(0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0, 0.0)
        y = np.array([row.label for row in rows], dtype=np.float32)
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

    def evaluation_diagnostics(
        self,
        backend: str,
        runtime_model,
        rows: list[TrainingRow],
    ) -> dict[str, float | int | dict[str, float]]:
        if not rows:
            return {}

        x = np.array([row.features for row in rows], dtype=np.float32)
        y = np.array([row.label for row in rows], dtype=np.float32)
        if backend == "lightgbm":
            predictions = runtime_model.predict(x)
        else:
            predictions = runtime_model.predict(x)

        return {
            "label_stats": _label_stats(y),
            "prediction_stats": _label_stats(np.array(predictions, dtype=np.float32)),
            "ranking": _ranking_diagnostics(rows, predictions),
        }


def _export_model(
    model: GradientBoostingRegressor,
    feature_names: list[str],
    importances: np.ndarray,
) -> dict[str, Any]:
    tree_info: list[dict[str, Any]] = []
    init_value = float(model.init_.constant_[0][0]) if hasattr(model.init_, "constant_") else 0.0
    tree_info.append({"shrinkage": 1.0, "tree_structure": {"leaf_value": init_value}})

    for idx in range(model.n_estimators_):
        tree = model.estimators_[idx, 0].tree_
        tree_info.append(
            {
                "shrinkage": model.learning_rate,
                "tree_structure": _tree_to_dict(tree, 0),
            }
        )

    return {
        "objective": "regression",
        "average_output": False,
        "feature_names": feature_names,
        "tree_info": tree_info,
        "feature_importances": dict(zip(feature_names, importances.tolist())),
    }


def _tree_to_dict(tree, node_id: int) -> dict[str, Any]:
    if tree.children_left[node_id] == -1:
        return {"leaf_value": float(tree.value[node_id][0, 0])}
    return {
        "split_feature": int(tree.feature[node_id]),
        "threshold": float(tree.threshold[node_id]),
        "decision_type": "<=",
        "default_left": True,
        "left_child": _tree_to_dict(tree, tree.children_left[node_id]),
        "right_child": _tree_to_dict(tree, tree.children_right[node_id]),
    }


def _validate_data(x: np.ndarray, y: np.ndarray, name: str) -> None:
    if len(x) == 0:
        raise ValueError(f"{name} set is empty")
    if np.any(np.isnan(x)):
        nan_columns = np.where(np.any(np.isnan(x), axis=0))[0]
        raise ValueError(f"{name} set has NaN values in feature columns: {nan_columns.tolist()}")
    if np.any(np.isinf(x)):
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


def _r2(actual: np.ndarray, predicted: np.ndarray) -> float:
    if len(actual) <= 1:
        return 0.0
    if float(np.var(actual)) == 0.0:
        return 0.0
    return float(r2_score(actual, predicted))


def _ndcg(rows: list[TrainingRow], predictions: np.ndarray, k: int = 10) -> float:
    from collections import defaultdict

    groups: dict[str, list[int]] = defaultdict(list)
    for idx, row in enumerate(rows):
        groups[row.viewer_id].append(idx)

    if not groups:
        return 0.0

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


def _slice_booster_to_best_iteration(booster, tree_count: int):
    try:
        if tree_count > 0 and tree_count < len(booster.get_dump()):
            return booster[:tree_count]
    except (TypeError, AttributeError):
        return booster
    return booster


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

    pairwise_accuracy = correct_pairs / comparable_pairs if comparable_pairs > 0 else 0.0
    return {
        "group_count": len(groups),
        "ranked_group_count": ranked_groups,
        "comparable_pair_count": comparable_pairs,
        "pairwise_accuracy": float(pairwise_accuracy),
    }


def _build_lgb_history(eval_log: dict[str, dict[str, list[float]]]) -> list[TrainingHistoryPoint]:
    """Convert LightGBM eval_log (recorded by lgb.record_evaluation) to history points."""
    train_rmse = eval_log.get("train", {}).get("rmse", [])
    valid_rmse = eval_log.get("valid", {}).get("rmse", [])
    train_mae  = eval_log.get("train", {}).get("mae",  [])
    valid_mae  = eval_log.get("valid", {}).get("mae",  [])
    count = max(len(train_rmse), len(valid_rmse), len(train_mae), len(valid_mae))

    history: list[TrainingHistoryPoint] = []
    for idx in range(count):
        history.append(
            TrainingHistoryPoint(
                iteration=idx + 1,
                train_rmse=train_rmse[idx] if idx < len(train_rmse) else None,
                validation_rmse=valid_rmse[idx] if idx < len(valid_rmse) else None,
                train_mae=train_mae[idx]  if idx < len(train_mae)  else None,
                validation_mae=valid_mae[idx]  if idx < len(valid_mae)  else None,
            )
        )
    return history
