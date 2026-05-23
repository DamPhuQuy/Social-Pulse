"""Feature engineering: builds training rows with interaction features."""
from __future__ import annotations

import math
import random
from bisect import bisect_left, bisect_right

import numpy as np

from ai_pipeline.shared.schema import RankingFeatureSchema
from .scanner import PushshiftDatasetScanner
from .types import AuthorAggregate, DatasetSplit, SubmissionRecord, TrainingDataset, TrainingRow

_SECONDS_PER_HOUR = 3600.0
_SECONDS_PER_DAY = 86400.0
_SECONDS_PER_YEAR = 365.0 * _SECONDS_PER_DAY
_NEGATIVE_LOOKBACK_HOURS = 72.0


class PushshiftFeatureEngineering:
    def build_training_dataset(
        self,
        sampled_posts: list[SubmissionRecord],
        author_aggregates: dict[str, AuthorAggregate],
        interactions: dict[str, dict[str, list[float]]],
        post_interactions: dict[str, dict[str, list[float]]],
        negative_samples_per_positive: int,
        max_positive_viewers_per_post: int,
        seed: int,
    ) -> TrainingDataset:
        rng = random.Random(seed)
        reference_utc = max((record.retrieved_on for record in sampled_posts), default=0.0)
        sorted_posts = sorted(sampled_posts, key=lambda record: (record.created_utc, record.post_id))
        post_created_times = [record.created_utc for record in sorted_posts]

        viewer_total_interactions = {
            viewer: sum(len(timestamps) for timestamps in authors.values())
            for viewer, authors in interactions.items()
        }
        viewer_positive_posts = _viewer_positive_posts(post_interactions)

        rows: list[TrainingRow] = []
        positive_rows = 0
        negative_rows = 0
        capped_positive_posts = 0
        zero_interaction = [0.0, 0.0, RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0]

        for record in sampled_posts:
            aggregate = record.author_snapshot or author_aggregates.get(record.author, AuthorAggregate.empty())
            popularity = PushshiftDatasetScanner.popularity(record.score, record.num_comments, record.num_crossposts)
            positive_label = _log1p(popularity)
            positive_items = sorted(
                post_interactions.get(record.post_id, {}).items(),
                key=lambda item: min(item[1]) if item[1] else float("inf"),
            )

            if max_positive_viewers_per_post > 0 and len(positive_items) > max_positive_viewers_per_post:
                positive_items = positive_items[:max_positive_viewers_per_post]
                capped_positive_posts += 1

            for viewer, post_timestamps in positive_items:
                post_comments = [timestamp for timestamp in post_timestamps if timestamp >= record.created_utc]
                if not post_comments:
                    continue

                query_utc = min(min(post_comments), reference_utc)
                author_timestamps = interactions.get(viewer, {}).get(record.author, [])
                base = self._build_base_features(record, aggregate, query_utc)
                interaction_features = self._compute_interaction_features(
                    author_timestamps,
                    record.created_utc,
                    viewer_total_interactions.get(viewer, 1),
                )
                rows.append(
                    TrainingRow(
                        post_id=record.post_id,
                        features=self._merge(base, interaction_features),
                        label=positive_label,
                        viewer_id=viewer,
                        created_utc=record.created_utc,
                        split_key=record.post_id,
                    )
                )
                positive_rows += 1

                negative_records = self._find_negative_posts_for_viewer(
                    sorted_posts=sorted_posts,
                    post_created_times=post_created_times,
                    viewer_positive_posts=viewer_positive_posts.get(viewer, set()),
                    positive_post_id=record.post_id,
                    query_utc=query_utc,
                    limit=negative_samples_per_positive,
                    rng=rng,
                )
                for negative_record in negative_records:
                    negative_aggregate = (
                        negative_record.author_snapshot
                        or author_aggregates.get(negative_record.author, AuthorAggregate.empty())
                    )
                    negative_base = self._build_base_features(negative_record, negative_aggregate, query_utc)
                    negative_author_timestamps = interactions.get(viewer, {}).get(negative_record.author, [])
                    negative_interaction_features = (
                        self._compute_interaction_features(
                            negative_author_timestamps,
                            negative_record.created_utc,
                            viewer_total_interactions.get(viewer, 1),
                        )
                        if negative_author_timestamps
                        else zero_interaction
                    )
                    rows.append(
                        TrainingRow(
                            post_id=negative_record.post_id,
                            features=self._merge(negative_base, negative_interaction_features),
                            label=0.0,
                            viewer_id=viewer,
                            created_utc=record.created_utc,
                            split_key=record.post_id,
                        )
                    )
                    negative_rows += 1

        if not rows:
            raise RuntimeError("Unable to build training rows from post-level interactions.")

        rows, preprocessing = self._preprocess_features(rows)
        self._validate_rows(rows)
        feature_stats = self._compute_feature_stats(rows)
        feature_stats["row_composition"] = {
            "positive_rows": positive_rows,
            "negative_rows": negative_rows,
            "fallback_rows": 0,
            "posts_with_post_level_interactions": len(post_interactions),
            "posts_without_post_level_interactions": max(len(sampled_posts) - len(post_interactions), 0),
            "posts_capped_by_max_positive_viewers": capped_positive_posts,
            "max_positive_viewers_per_post": max_positive_viewers_per_post,
            "negative_samples_per_positive": negative_samples_per_positive,
            "negative_sampling_strategy": "viewer_time_hard_negative",
            "negative_lookback_hours": _NEGATIVE_LOOKBACK_HOURS,
        }
        return TrainingDataset(rows, feature_stats, preprocessing)

    def split_rows(self, rows: list[TrainingRow], validation_ratio: float, test_ratio: float) -> DatasetSplit:
        split_groups: dict[str, list[TrainingRow]] = {}
        for row in sorted(rows, key=lambda r: (r.created_utc, r.split_key or r.post_id, r.viewer_id, r.post_id)):
            split_groups.setdefault(row.split_key or row.post_id, []).append(row)

        ordered_groups = sorted(
            split_groups.items(),
            key=lambda item: (item[1][0].created_utc, item[0]),
        )
        total_groups = len(ordered_groups)
        train_end = int(total_groups * (1.0 - validation_ratio - test_ratio))
        validation_end = int(total_groups * (1.0 - test_ratio))

        def flatten(items: list[tuple[str, list[TrainingRow]]]) -> list[TrainingRow]:
            return [row for _, group_rows in items for row in group_rows]

        train_rows = flatten(ordered_groups[:train_end])
        validation_rows = flatten(ordered_groups[train_end:validation_end])
        test_rows = flatten(ordered_groups[validation_end:])
        return DatasetSplit(train_rows, validation_rows, test_rows)

    def _preprocess_features(self, rows: list[TrainingRow]) -> tuple[list[TrainingRow], dict[str, object]]:
        if not rows:
            return rows, {
                "cap_percentile": RankingFeatureSchema.DEFAULT_CAP_PERCENTILE,
                "cap_values": {},
                "log_transform_features": list(RankingFeatureSchema.LOG_TRANSFORM_FEATURES),
            }

        feature_names = RankingFeatureSchema.FEATURE_ORDER
        matrix = np.array([row.features for row in rows], dtype=np.float32)
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
            TrainingRow(
                post_id=row.post_id,
                features=matrix[row_idx].tolist(),
                label=row.label,
                viewer_id=row.viewer_id,
                created_utc=row.created_utc,
                split_key=row.split_key,
            )
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
        query_utc: float,
    ) -> list[float]:
        """Build base features matching RankingFeatureSchema.FEATURE_ORDER.

        Target-time engagement snapshots are excluded because they would leak
        the label. Interaction slots are filled later by _merge().
        """
        author_seniority = 0.0
        if record.author_created_utc and record.author_created_utc > 0:
            author_seniority = max(record.created_utc - record.author_created_utc, 0.0) / _SECONDS_PER_YEAR
        return [
            record.title_length + record.body_length,
            1.0 if record.has_multimedia else 0.0,
            1.0 if record.is_share_post else 0.0,
            max(query_utc - record.created_utc, 0.0) / _SECONDS_PER_HOUR,
            author_seniority,
            aggregate.post_count,
            aggregate.average_popularity,
            0.0,
            0.0,
            0.0,
            0.0,
        ]

    @staticmethod
    def _merge(base: list[float], interaction_features: list[float]) -> list[float]:
        merged = list(base)
        merged[7] = interaction_features[0]
        merged[8] = interaction_features[1]
        merged[9] = interaction_features[2]
        merged[10] = interaction_features[3]
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
            if latest < timestamp < post_created_utc:
                latest = timestamp

        hours_since = (
            (post_created_utc - latest) / _SECONDS_PER_HOUR
            if latest > 0
            else RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS
        )
        affinity = count_30d / viewer_total if viewer_total > 0 else 0.0
        return [float(count_7d), float(count_30d), hours_since, affinity]

    @staticmethod
    def _find_negative_posts_for_viewer(
        sorted_posts: list[SubmissionRecord],
        post_created_times: list[float],
        viewer_positive_posts: set[str],
        positive_post_id: str,
        query_utc: float,
        limit: int,
        rng: random.Random,
    ) -> list[SubmissionRecord]:
        if limit <= 0 or not sorted_posts:
            return []

        window_start = query_utc - _NEGATIVE_LOOKBACK_HOURS * _SECONDS_PER_HOUR
        left = bisect_left(post_created_times, window_start)
        right = bisect_right(post_created_times, query_utc)
        if right <= left:
            left = 0
            right = bisect_right(post_created_times, query_utc)

        negatives: list[SubmissionRecord] = []
        selected_post_ids: set[str] = set()
        available_count = max(right - left, 0)
        max_attempts = max(100, limit * 50)

        for _ in range(max_attempts):
            if len(negatives) >= limit or available_count <= 0:
                break
            candidate = sorted_posts[rng.randrange(left, right)]
            if (
                candidate.post_id == positive_post_id
                or candidate.post_id in viewer_positive_posts
                or candidate.post_id in selected_post_ids
            ):
                continue
            selected_post_ids.add(candidate.post_id)
            negatives.append(candidate)

        if len(negatives) >= limit:
            return negatives

        for candidate in sorted_posts[left:right]:
            if len(negatives) >= limit:
                break
            if (
                candidate.post_id == positive_post_id
                or candidate.post_id in viewer_positive_posts
                or candidate.post_id in selected_post_ids
            ):
                continue
            selected_post_ids.add(candidate.post_id)
            negatives.append(candidate)
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
                    raise ValueError(f"Invalid value in row {row_idx}, feature '{feature_name}': {value}")


def _viewer_positive_posts(post_interactions: dict[str, dict[str, list[float]]]) -> dict[str, set[str]]:
    result: dict[str, set[str]] = {}
    for post_id, viewers in post_interactions.items():
        for viewer in viewers:
            result.setdefault(viewer, set()).add(post_id)
    return result


def _log1p(value: float) -> float:
    return math.log1p(value)
