"""Training pipeline orchestrator."""
from __future__ import annotations

from datetime import datetime, timezone
from pathlib import Path
from time import perf_counter

import numpy as np

from ai_pipeline.shared.schema import RankingFeatureSchema
from . import json_support as js
from .arguments import TrainingArguments
from .feature_engineering import PushshiftFeatureEngineering
from .scanner import PushshiftDatasetScanner
from .system_info import collect_runtime_info
from .trainer import GradientBoostedTreeTrainer
from .types import Metrics, TrainingRunResult
from .visualization import generate_training_visualizations

_DATASET_NAME = "pushshift_reddit_apr2019"
_LABEL_STRATEGY = "log_popularity_proxy_personalized"


class PushshiftTrainingPipeline:
    def __init__(self):
        self._scanner = PushshiftDatasetScanner()
        self._feature_eng = PushshiftFeatureEngineering()
        self._trainer = GradientBoostedTreeTrainer()

    def run(self, arguments: TrainingArguments) -> TrainingRunResult:
        arguments.validate()
        started_at = perf_counter()

        scan_result = self._scanner.scan_submissions(arguments)
        if len(scan_result.sampled_posts) < 512:
            raise RuntimeError(f"Not enough cleaned submissions: {len(scan_result.sampled_posts)}")

        post_author_map = {post.post_id: post.author for post in scan_result.sampled_posts}

        interactions: dict = {}
        interaction_stats = {"comments_scanned": 0, "interactions_extracted": 0}
        if arguments.comments_path:
            interaction_result = self._scanner.scan_interactions(
                arguments.comments_path,
                post_author_map,
                arguments.scan_limit_comments,
                arguments,
            )
            interactions = interaction_result.interactions
            interaction_stats = interaction_result.stats

        dataset = self._feature_eng.build_training_dataset(
            scan_result.sampled_posts,
            scan_result.author_aggregates,
            interactions,
            arguments.negative_samples_per_post,
        )
        split = self._feature_eng.split_rows(dataset.rows, arguments.validation_ratio, arguments.test_ratio)
        if not split.train_rows or not split.validation_rows or not split.test_rows:
            raise RuntimeError("Unable to build train, validation, and test splits.")

        trained_model = self._trainer.train(arguments, split.train_rows, split.validation_rows)
        test_metrics = self._trainer.evaluate(trained_model.backend, trained_model.runtime_model, split.test_rows)
        train_label_mean = float(np.mean([row.label for row in split.train_rows]))
        validation_baseline_metrics = self._trainer.evaluate_baseline(split.validation_rows, train_label_mean)
        test_baseline_metrics = self._trainer.evaluate_baseline(split.test_rows, train_label_mean)
        evaluation_diagnostics = {
            "train": self._trainer.evaluation_diagnostics(
                trained_model.backend,
                trained_model.runtime_model,
                split.train_rows,
            ),
            "validation": self._trainer.evaluation_diagnostics(
                trained_model.backend,
                trained_model.runtime_model,
                split.validation_rows,
            ),
            "test": self._trainer.evaluation_diagnostics(
                trained_model.backend,
                trained_model.runtime_model,
                split.test_rows,
            ),
            "mean_label_baseline": {
                "prediction_value": train_label_mean,
                "validation": _metrics_to_test_dict(validation_baseline_metrics),
                "test": _metrics_to_test_dict(test_baseline_metrics),
            },
        }
        evaluation_warnings = _build_evaluation_warnings(evaluation_diagnostics, final_metrics=None)
        final_metrics = Metrics(
            train_rmse=trained_model.metrics.train_rmse,
            validation_rmse=trained_model.metrics.validation_rmse,
            test_rmse=test_metrics.test_rmse,
            train_mae=trained_model.metrics.train_mae,
            validation_mae=trained_model.metrics.validation_mae,
            test_mae=test_metrics.test_mae,
            train_ndcg_k=trained_model.metrics.train_ndcg_k,
            validation_ndcg_k=trained_model.metrics.validation_ndcg_k,
            test_ndcg_k=test_metrics.test_ndcg_k,
            train_r2=trained_model.metrics.train_r2,
            validation_r2=trained_model.metrics.validation_r2,
            test_r2=test_metrics.test_r2,
        )
        evaluation_warnings = _build_evaluation_warnings(evaluation_diagnostics, final_metrics)

        trained_at = datetime.now(timezone.utc).isoformat()
        plots_output_dir = arguments.plots_output_dir or (arguments.output_path.parent / "plots")
        plot_paths = generate_training_visualizations(
            plots_output_dir,
            dataset.rows,
            trained_model.history,
            trained_model.feature_importances,
        )
        duration_seconds = round(perf_counter() - started_at, 3)

        summary = self._build_summary(
            arguments=arguments,
            scan_stats=scan_result.scan_stats,
            interaction_stats=interaction_stats,
            feature_stats=dataset.feature_stats,
            preprocessing=dataset.preprocessing,
            train_rows=len(split.train_rows),
            validation_rows=len(split.validation_rows),
            test_rows=len(split.test_rows),
            metrics=final_metrics,
            feature_importances=trained_model.feature_importances,
            history=trained_model.history,
            plot_paths=plot_paths,
            duration_seconds=duration_seconds,
            runtime_info=collect_runtime_info(),
            best_iteration=trained_model.best_iteration,
            model_backend=trained_model.backend,
            evaluation_diagnostics=evaluation_diagnostics,
            evaluation_warnings=evaluation_warnings,
        )
        artifact = self._build_artifact(
            trained_at=trained_at,
            summary=summary,
            preprocessing=dataset.preprocessing,
            model_backend=trained_model.backend,
            model_dump=trained_model.model_dump,
        )

        self._persist_runtime_model(arguments.output_path, trained_model.backend, trained_model.runtime_model)
        js.write_json(arguments.output_path, artifact)
        if arguments.metrics_output_path:
            js.write_json(arguments.metrics_output_path, summary)

        return TrainingRunResult(
            arguments.output_path,
            trained_at,
            final_metrics,
            len(split.train_rows),
            len(split.validation_rows),
            len(split.test_rows),
            trained_model.backend,
            evaluation_warnings,
        )

    @staticmethod
    def _build_summary(
        arguments: TrainingArguments,
        scan_stats: dict,
        interaction_stats: dict,
        feature_stats: dict,
        preprocessing: dict,
        train_rows: int,
        validation_rows: int,
        test_rows: int,
        metrics: Metrics,
        feature_importances: dict[str, float],
        history: list,
        plot_paths: dict[str, str],
        duration_seconds: float,
        runtime_info: dict,
        best_iteration: int | None,
        model_backend: str,
        evaluation_diagnostics: dict,
        evaluation_warnings: list[str],
    ) -> dict:
        return {
            "scan_stats": scan_stats,
            "interaction_stats": interaction_stats,
            "feature_stats": feature_stats,
            "preprocessing": preprocessing,
            "train_rows": train_rows,
            "validation_rows": validation_rows,
            "test_rows": test_rows,
            "metrics": {
                "train_rmse": metrics.train_rmse,
                "validation_rmse": metrics.validation_rmse,
                "test_rmse": metrics.test_rmse,
                "train_mae": metrics.train_mae,
                "validation_mae": metrics.validation_mae,
                "test_mae": metrics.test_mae,
                "train_ndcg_k": metrics.train_ndcg_k,
                "validation_ndcg_k": metrics.validation_ndcg_k,
                "test_ndcg_k": metrics.test_ndcg_k,
                "train_r2": metrics.train_r2,
                "validation_r2": metrics.validation_r2,
                "test_r2": metrics.test_r2,
            },
            "hyperparameters": {
                "sample_size": arguments.sample_size,
                "scan_limit_posts": arguments.scan_limit_posts,
                "scan_limit_comments": arguments.scan_limit_comments,
                "min_content_length": arguments.min_content_length,
                "max_content_length": arguments.max_content_length,
                "exclude_nsfw": arguments.exclude_nsfw,
                "dedupe_posts": arguments.dedupe_posts,
                "filter_bots": arguments.filter_bots,
                "min_distinct_token_count": arguments.min_distinct_token_count,
                "min_alpha_char_count": arguments.min_alpha_char_count,
                "max_url_count": arguments.max_url_count,
                "n_estimators": arguments.n_estimators,
                "max_depth": arguments.max_depth,
                "min_samples_leaf": arguments.min_samples_leaf,
                "min_child_weight": arguments.min_child_weight,
                "learning_rate": arguments.learning_rate,
                "subsample": arguments.subsample,
                "colsample_bytree": arguments.colsample_bytree,
                "reg_lambda": arguments.reg_lambda,
                "reg_alpha": arguments.reg_alpha,
                "max_bin": arguments.max_bin,
                "early_stopping_rounds": arguments.early_stopping_rounds,
                "validation_ratio": arguments.validation_ratio,
                "test_ratio": arguments.test_ratio,
                "negative_samples_per_post": arguments.negative_samples_per_post,
                "trainer_backend": arguments.trainer_backend,
                "device": arguments.device,
                "n_jobs": arguments.n_jobs,
                "allow_cpu_fallback": arguments.allow_cpu_fallback,
                "seed": arguments.seed,
            },
            "split_strategy": "time_ordered_train_validation_test",
            "runtime": {
                "duration_seconds": duration_seconds,
                "hardware": runtime_info,
                "best_iteration": best_iteration,
                "model_backend": model_backend,
            },
            "visualizations": plot_paths,
            "history": [
                {
                    "iteration": point.iteration,
                    "train_rmse": point.train_rmse,
                    "validation_rmse": point.validation_rmse,
                    "train_mae": point.train_mae,
                    "validation_mae": point.validation_mae,
                }
                for point in history
            ],
            "feature_importances": feature_importances,
            "evaluation_diagnostics": evaluation_diagnostics,
            "evaluation_warnings": evaluation_warnings,
        }

    @staticmethod
    def _build_artifact(
        trained_at: str,
        summary: dict,
        preprocessing: dict,
        model_backend: str,
        model_dump: dict | None,
    ) -> dict:
        artifact = {
            "artifact_version": "1",
            "feature_schema_version": RankingFeatureSchema.DEFAULT_SCHEMA_VERSION,
            "training_dataset": _DATASET_NAME,
            "trained_at": trained_at,
            "label_strategy": _LABEL_STRATEGY,
            "model_backend": model_backend,
            "preprocessing": preprocessing,
            "training_summary": summary,
        }
        if model_backend == "lightgbm":
            artifact["model_file"] = "model.txt"
        else:
            artifact["model_dump"] = model_dump
        return artifact

    @staticmethod
    def _persist_runtime_model(output_path: Path, model_backend: str, runtime_model) -> None:
        if model_backend != "lightgbm":
            return
        sidecar_path = output_path.with_suffix(".txt")
        sidecar_path.parent.mkdir(parents=True, exist_ok=True)
        runtime_model.save_model(str(sidecar_path))


def _metrics_to_test_dict(metrics: Metrics) -> dict[str, float]:
    return {
        "rmse": metrics.test_rmse,
        "mae": metrics.test_mae,
        "ndcg_k": metrics.test_ndcg_k,
        "r2": metrics.test_r2,
    }


def _build_evaluation_warnings(evaluation_diagnostics: dict, final_metrics: Metrics | None) -> list[str]:
    warnings: list[str] = []
    train = evaluation_diagnostics.get("train", {})
    validation = evaluation_diagnostics.get("validation", {})
    test = evaluation_diagnostics.get("test", {})

    train_label = train.get("label_stats", {})
    validation_label = validation.get("label_stats", {})
    test_label = test.get("label_stats", {})
    test_ranking = test.get("ranking", {})

    if test_ranking.get("ranked_group_count", 0) < 1000:
        warnings.append(
            "test ranking metrics have low support; fewer than 1000 test groups contain mixed labels"
        )
    if test_label.get("zero_ratio", 0.0) > 0.9:
        warnings.append("test labels are highly sparse; more than 90% of test rows have zero relevance")
    if train_label and test_label:
        train_mean = float(train_label.get("mean", 0.0))
        test_mean = float(test_label.get("mean", 0.0))
        train_std = max(float(train_label.get("std", 0.0)), 1e-6)
        if abs(train_mean - test_mean) > train_std * 0.5:
            warnings.append("train/test label distribution is shifted; read test R2 together with baseline metrics")
    if validation_label and test_label:
        validation_mean = float(validation_label.get("mean", 0.0))
        test_mean = float(test_label.get("mean", 0.0))
        validation_std = max(float(validation_label.get("std", 0.0)), 1e-6)
        if abs(validation_mean - test_mean) > validation_std * 0.25:
            warnings.append("validation/test label distribution is shifted; pilot scan limits may be too small")
    if final_metrics and final_metrics.test_r2 < 0.0:
        warnings.append("test R2 is negative; the model underperforms the test-set mean baseline on absolute labels")

    return warnings
