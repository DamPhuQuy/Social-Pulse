"""Training pipeline orchestrator."""
from __future__ import annotations

from datetime import datetime, timezone

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from . import json_support as js
from .arguments import TrainingArguments
from .feature_engineering import PushshiftFeatureEngineering
from .scanner import PushshiftDatasetScanner
from .trainer import GradientBoostedTreeTrainer
from .types import Metrics, TrainingRunResult

_DATASET_NAME = "pushshift_reddit_apr2019"
_LABEL_STRATEGY = "log_popularity_proxy_personalized"
_NEGATIVE_SAMPLES_PER_POST = 3


class PushshiftTrainingPipeline:
    def __init__(self):
        self._scanner = PushshiftDatasetScanner()
        self._feature_eng = PushshiftFeatureEngineering()
        self._trainer = GradientBoostedTreeTrainer()

    def run(self, arguments: TrainingArguments) -> TrainingRunResult:
        arguments.validate()

        scan_result = self._scanner.scan_submissions(arguments)
        if len(scan_result.sampled_posts) < 512:
            raise RuntimeError(f"Not enough cleaned submissions: {len(scan_result.sampled_posts)}")

        post_author_map = {p.post_id: p.author for p in scan_result.sampled_posts}

        interactions: dict = {}
        interaction_stats = {"comments_scanned": 0, "interactions_extracted": 0}
        if arguments.comments_path:
            result = self._scanner.scan_interactions(
                arguments.comments_path, post_author_map, arguments.scan_limit_comments
            )
            interactions = result.interactions
            interaction_stats = result.stats

        dataset = self._feature_eng.build_training_dataset(
            scan_result.sampled_posts, scan_result.author_aggregates,
            interactions, _NEGATIVE_SAMPLES_PER_POST,
        )
        split = self._feature_eng.split_rows(dataset.rows)
        if not split.train_rows or not split.validation_rows:
            raise RuntimeError("Unable to build both train and validation splits.")

        model = self._trainer.train(arguments, split.train_rows, split.validation_rows)

        trained_at = datetime.now(timezone.utc).isoformat()
        summary = self._build_summary(
            arguments, scan_result.scan_stats, interaction_stats,
            dataset.feature_stats, len(split.train_rows), len(split.validation_rows), model.metrics,
        )
        artifact = self._build_artifact(trained_at, summary, model.model_dump)

        js.write_json(arguments.output_path, artifact)
        if arguments.metrics_output_path:
            js.write_json(arguments.metrics_output_path, summary)

        return TrainingRunResult(arguments.output_path, trained_at, model.metrics, len(split.train_rows), len(split.validation_rows))

    @staticmethod
    def _build_summary(
        arguments: TrainingArguments, scan_stats: dict, interaction_stats: dict,
        feature_stats: dict, train_rows: int, validation_rows: int, metrics: Metrics,
    ) -> dict:
        return {
            "scan_stats": scan_stats,
            "interaction_stats": interaction_stats,
            "feature_stats": feature_stats,
            "train_rows": train_rows,
            "validation_rows": validation_rows,
            "metrics": {
                "train_rmse": metrics.train_rmse,
                "validation_rmse": metrics.validation_rmse,
                "train_mae": metrics.train_mae,
                "validation_mae": metrics.validation_mae,
            },
            "hyperparameters": {
                "sample_size": arguments.sample_size,
                "scan_limit_posts": arguments.scan_limit_posts,
                "scan_limit_comments": arguments.scan_limit_comments,
                "n_estimators": arguments.n_estimators,
                "max_depth": arguments.max_depth,
                "min_samples_leaf": arguments.min_samples_leaf,
                "max_thresholds": arguments.max_thresholds,
                "learning_rate": arguments.learning_rate,
                "seed": arguments.seed,
            },
        }

    @staticmethod
    def _build_artifact(trained_at: str, summary: dict, model_dump: dict) -> dict:
        return {
            "artifact_version": "1",
            "feature_schema_version": LightGbmFeatureSchema.DEFAULT_SCHEMA_VERSION,
            "training_dataset": _DATASET_NAME,
            "trained_at": trained_at,
            "label_strategy": _LABEL_STRATEGY,
            "training_summary": summary,
            "model_dump": model_dump,
        }
