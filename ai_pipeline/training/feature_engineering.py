"""Feature engineering: builds training rows with interaction features."""
from __future__ import annotations

import hashlib

from ai_pipeline.shared.schema import LightGbmFeatureSchema
from .scanner import PushshiftDatasetScanner
from .types import (
    AuthorAggregate, DatasetSplit, SubmissionRecord, TrainingDataset, TrainingRow,
)

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

            # Positive rows
            author_interactors = self._find_viewers_for_author(interactions, record.author)
            for viewer, timestamps in author_interactors.items():
                interaction_feats = self._compute_interaction_features(
                    timestamps, record.created_utc, viewer_total_interactions.get(viewer, 1)
                )
                rows.append(TrainingRow(record.post_id, self._merge(base, interaction_feats), _log1p(popularity)))

            # Negative rows
            negative_viewers = self._find_negative_viewers(interactions, record.author, negative_samples_per_post)
            zero_interaction = [0.0, 0.0, LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS, 0.0]
            for _ in negative_viewers:
                rows.append(TrainingRow(record.post_id, self._merge(base, zero_interaction), 0.0))

            # Fallback
            if not author_interactors and not negative_viewers:
                rows.append(TrainingRow(record.post_id, self._merge(base, zero_interaction), _log1p(popularity)))

        return TrainingDataset(rows, {"total_training_rows": len(rows), "reference_utc": reference_utc})

    def split_rows(self, rows: list[TrainingRow]) -> DatasetSplit:
        train, val = [], []
        for row in rows:
            (val if self._bucket_for_post_id(row.post_id) == 0 else train).append(row)
        return DatasetSplit(train, val)

    # ─── Private ──────────────────────────────────────────────────────────

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
    def _bucket_for_post_id(post_id: str) -> int:
        hashed = hashlib.md5(post_id.encode("utf-8")).digest()
        value = (hashed[0] << 24) | (hashed[1] << 16) | (hashed[2] << 8) | hashed[3]
        return value % 5


def _log1p(x: float) -> float:
    import math
    return math.log1p(x)
