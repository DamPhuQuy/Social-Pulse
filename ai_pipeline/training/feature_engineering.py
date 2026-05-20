"""Feature engineering: builds training rows with interaction features."""
from __future__ import annotations

import math

import numpy as np

from ai_pipeline.shared.schema import RankingFeatureSchema
from .scanner import PushshiftDatasetScanner
from .types import AuthorAggregate, DatasetSplit, SubmissionRecord, TrainingDataset, TrainingRow

_SECONDS_PER_HOUR = 3600.0
_SECONDS_PER_DAY = 86400.0
_SECONDS_PER_YEAR = 365.0 * _SECONDS_PER_DAY


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

            author_interactors = self._find_viewers_for_author(interactions, record.author)
            for viewer, timestamps in author_interactors.items():
                interaction_feats = self._compute_interaction_features(
                    timestamps,
                    record.created_utc,
                    viewer_total_interactions.get(viewer, 1),
                )
                rows.append(
                    TrainingRow(
                        record.post_id,
                        self._merge(base, interaction_feats),
                        _log1p(popularity),
                        record.created_utc,
                    )
                )

            negative_viewers = self._find_negative_viewers(interactions, record.author, negative_samples_per_post)
            zero_interaction = [0.0, 0.0, RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0]
            for _ in negative_viewers:
                rows.append(
                    TrainingRow(record.post_id, self._merge(base, zero_interaction), 0.0, record.created_utc)
                )

            if not author_interactors and not negative_viewers:
                rows.append(
                    TrainingRow(
                        record.post_id,
                        self._merge(base, zero_interaction),
                        _log1p(popularity),
                        record.created_utc,
                    )
                )

        rows, preprocessing = self._preprocess_features(rows)
        self._validate_rows(rows)
        feature_stats = self._compute_feature_stats(rows)
        return TrainingDataset(rows, feature_stats, preprocessing)

    def split_rows(self, rows: list[TrainingRow], validation_ratio: float, test_ratio: float) -> DatasetSplit:
        sorted_rows = sorted(rows, key=lambda r: r.created_utc)
        total = len(sorted_rows)
        train_end = int(total * (1.0 - validation_ratio - test_ratio))
        validation_end = int(total * (1.0 - test_ratio))
        train_rows = sorted_rows[:train_end]
        validation_rows = sorted_rows[train_end:validation_end]
        test_rows = sorted_rows[validation_end:]
        return DatasetSplit(train_rows, validation_rows, test_rows)

    def _preprocess_features(self, rows: list[TrainingRow]) -> tuple[list[TrainingRow], dict[str, object]]:
        if not rows:
            return rows, {
                "cap_percentile": RankingFeatureSchema.DEFAULT_CAP_PERCENTILE,
                "cap_values": {},
                "log_transform_features": list(RankingFeatureSchema.LOG_TRANSFORM_FEATURES),
            }

        feature_names = RankingFeatureSchema.FEATURE_ORDER
        matrix = np.array([r.features for r in rows], dtype=np.float32)
        cap_values: dict[str, float] = {}

        for idx, name in enumerate(feature_names):
            if name in RankingFeatureSchema.CAP_FEATURES:
                cap = float(np.percentile(matrix[:, idx], RankingFeatureSchema.DEFAULT_CAP_PERCENTILE))
                if cap > 0:
                    matrix[:, idx] = np.minimum(matrix[:, idx], cap)
                    cap_values[name] = cap

        for idx, name in enumerate(feature_names):
            if name in RankingFeatureSchema.LOG_TRANSFORM_FEATURES:
                matrix[:, idx] = np.log1p(np.maximum(matrix[:, idx], 0.0))

        processed_rows = [
            TrainingRow(row.post_id, matrix[row_idx].tolist(), row.label, row.created_utc)
            for row_idx, row in enumerate(rows)
        ]
        preprocessing = {
            "cap_percentile": RankingFeatureSchema.DEFAULT_CAP_PERCENTILE,
            "cap_values": {key: round(float(value), 6) for key, value in cap_values.items()},
            "log_transform_features": list(RankingFeatureSchema.LOG_TRANSFORM_FEATURES),
        }
        return processed_rows, preprocessing

    @staticmethod
    def _compute_feature_stats(rows: list[TrainingRow]) -> dict:
        if not rows:
            return {"total_training_rows": 0}

        feature_names = RankingFeatureSchema.FEATURE_ORDER
        matrix = np.array([row.features for row in rows], dtype=np.float32)
        labels = np.array([row.label for row in rows], dtype=np.float32)

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

        for idx, name in enumerate(feature_names):
            column = matrix[:, idx]
            stats["feature_stats"][name] = {
                "mean": round(float(column.mean()), 4),
                "std": round(float(column.std()), 4),
                "min": round(float(column.min()), 4),
                "max": round(float(column.max()), 4),
                "zero_ratio": round(float((column == 0).sum() / len(column)), 4),
            }

        return stats

    def _build_base_features(
        self,
        record: SubmissionRecord,
        aggregate: AuthorAggregate,
        reference_utc: float,
    ) -> list[float]:
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
            0.0,
            0.0,
            0.0,
            0.0,
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
    def _compute_interaction_features(
        timestamps: list[float],
        post_created_utc: float,
        viewer_total: int,
    ) -> list[float]:
        count_7d = 0
        count_30d = 0
        latest = 0.0
        seven_days_before = post_created_utc - 7 * _SECONDS_PER_DAY
        thirty_days_before = post_created_utc - 30 * _SECONDS_PER_DAY

        for timestamp in timestamps:
            if seven_days_before <= timestamp < post_created_utc:
                count_7d += 1
            if thirty_days_before <= timestamp < post_created_utc:
                count_30d += 1
            if timestamp > latest and timestamp < post_created_utc:
                latest = timestamp

        hours_since = (
            (post_created_utc - latest) / _SECONDS_PER_HOUR
            if latest > 0
            else RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS
        )
        affinity = count_30d / viewer_total if viewer_total > 0 else 0.0
        return [float(count_7d), float(count_30d), hours_since, affinity]

    @staticmethod
    def _find_viewers_for_author(
        interactions: dict[str, dict[str, list[float]]],
        author: str,
    ) -> dict[str, list[float]]:
        result: dict[str, list[float]] = {}
        for viewer, authors in interactions.items():
            timestamps = authors.get(author)
            if timestamps:
                result[viewer] = timestamps
        return result

    @staticmethod
    def _find_negative_viewers(
        interactions: dict[str, dict[str, list[float]]],
        author: str,
        limit: int,
    ) -> list[str]:
        negatives: list[str] = []
        for viewer, authors in interactions.items():
            if author not in authors:
                negatives.append(viewer)
                if len(negatives) >= limit:
                    break
        return negatives

    @staticmethod
    def _validate_rows(rows: list[TrainingRow]) -> None:
        for row_idx, row in enumerate(rows):
            for feature_idx, value in enumerate(row.features):
                if math.isnan(value) or math.isinf(value):
                    feature_name = (
                        RankingFeatureSchema.FEATURE_ORDER[feature_idx]
                        if feature_idx < len(RankingFeatureSchema.FEATURE_ORDER)
                        else f"col_{feature_idx}"
                    )
                    raise ValueError(
                        f"Invalid value in row {row_idx}, feature '{feature_name}': {value}"
                    )


def _log1p(value: float) -> float:
    return math.log1p(value)
