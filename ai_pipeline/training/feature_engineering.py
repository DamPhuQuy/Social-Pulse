"""Feature engineering: builds training rows with interaction features."""
from __future__ import annotations

import math
from dataclasses import dataclass, field

import numpy as np

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from .scanner import PushshiftDatasetScanner
from .types import (
    AuthorAggregate, DatasetSplit, SubmissionRecord, TrainingDataset, TrainingRow,
)

_SECONDS_PER_HOUR = 3600.0
_SECONDS_PER_DAY = 86400.0
_SECONDS_PER_YEAR = 365.0 * _SECONDS_PER_DAY
_VALIDATION_RATIO = 0.2

# Percentile for outlier capping (winsorization)
_OUTLIER_PERCENTILE = 99.0

# Features that should be log1p-transformed (highly skewed count distributions)
_LOG_TRANSFORM_FEATURES = {
    "upvote_count", "downvote_count", "comment_count",
    "share_count", "view_count", "popularity",
    "interaction_count_7d", "interaction_count_30d",
}

# Features that should be capped at a percentile (outlier-prone)
_CAP_FEATURES = {
    "content_length", "post_age_hours", "hot_score",
    "author_seniority", "author_post_count", "author_engagement_rate",
    "hours_since_last_interaction",
}


class PushshiftFeatureEngineering:

    def build_training_dataset(
        self,
        sampled_posts: list[SubmissionRecord],
        author_aggregates: dict[str, AuthorAggregate],
        interactions: dict[str, dict[str, list[float]]],
        negative_samples_per_post: int,
    ) -> TrainingDataset:
        reference_utc = max((r.retrieved_on for r in sampled_posts), default=0.0)

        viewer_total_interactions: dict[str, int] = {}
        for viewer, authors in interactions.items():
            viewer_total_interactions[viewer] = sum(len(ts) for ts in authors.values())

        rows: list[TrainingRow] = []

        for record in sampled_posts:
            aggregate = author_aggregates.get(record.author, AuthorAggregate.empty())
            popularity = PushshiftDatasetScanner.popularity(record.score, record.num_comments, record.num_crossposts)
            base = self._build_base_features(record, aggregate, reference_utc)

            # Positive rows
            author_interactors = self._find_viewers_for_author(interactions, record.author)
            for viewer, timestamps in author_interactors.items():
                interaction_feats = self._compute_interaction_features(
                    timestamps, record.created_utc, viewer_total_interactions.get(viewer, 1)
                )
                rows.append(TrainingRow(record.post_id, self._merge(base, interaction_feats), _log1p(popularity), record.created_utc))

            # Negative rows
            negative_viewers = self._find_negative_viewers(interactions, record.author, negative_samples_per_post)
            zero_interaction = [0.0, 0.0, LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0]
            for _ in negative_viewers:
                rows.append(TrainingRow(record.post_id, self._merge(base, zero_interaction), 0.0, record.created_utc))

            # Fallback
            if not author_interactors and not negative_viewers:
                rows.append(TrainingRow(record.post_id, self._merge(base, zero_interaction), _log1p(popularity), record.created_utc))

        # Preprocessing: outlier capping + log transform
        rows = self._preprocess_features(rows)

        # Validate
        self._validate_rows(rows)

        # Compute distribution stats for monitoring
        feature_stats = self._compute_feature_stats(rows)

        return TrainingDataset(rows, feature_stats)

    def split_rows(self, rows: list[TrainingRow]) -> DatasetSplit:
        """Temporal split: oldest 80% for training, newest 20% for validation.

        This prevents temporal data leakage — the model never trains on future data.
        """
        sorted_rows = sorted(rows, key=lambda r: r.created_utc)
        split_idx = int(len(sorted_rows) * (1 - _VALIDATION_RATIO))
        return DatasetSplit(sorted_rows[:split_idx], sorted_rows[split_idx:])

    # ─── Preprocessing ────────────────────────────────────────────────────

    def _preprocess_features(self, rows: list[TrainingRow]) -> list[TrainingRow]:
        """Apply outlier capping and log-transform to skewed features."""
        if not rows:
            return rows

        feature_names = LightGbmFeatureSchema.FEATURE_ORDER
        n_features = len(feature_names)
        matrix = np.array([r.features for r in rows])

        # Step 1: Outlier capping (winsorize at 99th percentile)
        for i, name in enumerate(feature_names):
            if name in _CAP_FEATURES:
                cap = float(np.percentile(matrix[:, i], _OUTLIER_PERCENTILE))
                if cap > 0:
                    matrix[:, i] = np.minimum(matrix[:, i], cap)

        # Step 2: Log-transform skewed count features
        for i, name in enumerate(feature_names):
            if name in _LOG_TRANSFORM_FEATURES:
                matrix[:, i] = np.log1p(np.maximum(matrix[:, i], 0.0))

        # Rebuild rows with preprocessed features
        return [
            TrainingRow(r.post_id, matrix[idx].tolist(), r.label, r.created_utc)
            for idx, r in enumerate(rows)
        ]

    @staticmethod
    def _compute_feature_stats(rows: list[TrainingRow]) -> dict:
        """Compute distribution statistics for data quality monitoring."""
        if not rows:
            return {"total_training_rows": 0}

        feature_names = LightGbmFeatureSchema.FEATURE_ORDER
        matrix = np.array([r.features for r in rows])
        labels = np.array([r.label for r in rows])

        stats: dict = {
            "total_training_rows": len(rows),
            "label_stats": {
                "mean": float(labels.mean()),
                "std": float(labels.std()),
                "min": float(labels.min()),
                "max": float(labels.max()),
                "zero_ratio": float((labels == 0).sum() / len(labels)),
            },
            "feature_stats": {},
        }

        for i, name in enumerate(feature_names):
            col = matrix[:, i]
            stats["feature_stats"][name] = {
                "mean": round(float(col.mean()), 4),
                "std": round(float(col.std()), 4),
                "min": round(float(col.min()), 4),
                "max": round(float(col.max()), 4),
                "zero_ratio": round(float((col == 0).sum() / len(col)), 4),
            }

        return stats

    # ─── Feature Construction ─────────────────────────────────────────────

    def _build_base_features(self, record: SubmissionRecord, aggregate: AuthorAggregate, reference_utc: float) -> list[float]:
        author_seniority = 0.0
        if record.author_created_utc and record.author_created_utc > 0:
            author_seniority = max(record.created_utc - record.author_created_utc, 0.0) / _SECONDS_PER_YEAR
        popularity = PushshiftDatasetScanner.popularity(record.score, record.num_comments, record.num_crossposts)
        return [
            record.title_length + record.body_length,
            1.0 if record.has_multimedia else 0.0,
            1.0 if record.is_share_post else 0.0,
            max(reference_utc - record.created_utc, 0.0) / _SECONDS_PER_HOUR,
            record.hot_score,
            record.upvote_ratio,
            author_seniority,
            aggregate.post_count,
            aggregate.average_popularity,
            0.0, 0.0, 0.0, 0.0,  # slots 9-12 filled by merge
            max(record.score, 0),
            0.0,
            record.num_comments,
            record.num_crossposts,
            0.0,
            popularity,
        ]

    @staticmethod
    def _merge(base: list[float], interaction_features: list[float]) -> list[float]:
        merged = list(base)
        merged[9] = interaction_features[0]
        merged[10] = interaction_features[1]
        merged[11] = interaction_features[2]
        merged[12] = interaction_features[3]
        return merged

    @staticmethod
    def _compute_interaction_features(timestamps: list[float], post_created_utc: float, viewer_total: int) -> list[float]:
        count_7d = count_30d = 0
        latest = 0.0
        seven_days_before = post_created_utc - 7 * _SECONDS_PER_DAY
        thirty_days_before = post_created_utc - 30 * _SECONDS_PER_DAY

        for ts in timestamps:
            if seven_days_before <= ts < post_created_utc:
                count_7d += 1
            if thirty_days_before <= ts < post_created_utc:
                count_30d += 1
            if ts > latest and ts < post_created_utc:
                latest = ts

        hours_since = (post_created_utc - latest) / _SECONDS_PER_HOUR if latest > 0 else LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS
        affinity = count_30d / viewer_total if viewer_total > 0 else 0.0
        return [float(count_7d), float(count_30d), hours_since, affinity]

    @staticmethod
    def _find_viewers_for_author(interactions: dict[str, dict[str, list[float]]], author: str) -> dict[str, list[float]]:
        result = {}
        for viewer, authors in interactions.items():
            ts = authors.get(author)
            if ts:
                result[viewer] = ts
        return result

    @staticmethod
    def _find_negative_viewers(interactions: dict[str, dict[str, list[float]]], author: str, limit: int) -> list[str]:
        negatives = []
        for viewer, authors in interactions.items():
            if author not in authors:
                negatives.append(viewer)
                if len(negatives) >= limit:
                    break
        return negatives

    @staticmethod
    def _validate_rows(rows: list[TrainingRow]) -> None:
        """Validate that feature vectors have no NaN/Inf values."""
        for i, row in enumerate(rows):
            for j, val in enumerate(row.features):
                if math.isnan(val) or math.isinf(val):
                    feature_name = LightGbmFeatureSchema.FEATURE_ORDER[j] if j < len(LightGbmFeatureSchema.FEATURE_ORDER) else f"col_{j}"
                    raise ValueError(
                        f"Invalid value in row {i}, feature '{feature_name}': {val}"
                    )


def _log1p(x: float) -> float:
    return math.log1p(x)
