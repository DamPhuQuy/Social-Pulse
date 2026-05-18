"""Training data types - Python equivalents of Java records."""
from __future__ import annotations

from dataclasses import dataclass, field
from pathlib import Path
from typing import Any


@dataclass(frozen=True)
class SubmissionRecord:
    post_id: str
    author: str
    author_created_utc: float | None
    created_utc: float
    retrieved_on: float
    title_length: int
    body_length: int
    score: int
    num_comments: int
    num_crossposts: int
    has_multimedia: bool
    is_share_post: bool
    hot_score: float
    upvote_ratio: float


class AuthorAggregate:
    def __init__(self):
        self._post_count = 0
        self._cumulative_popularity = 0.0

    def increment(self, popularity: float) -> None:
        self._post_count += 1
        self._cumulative_popularity += popularity

    @property
    def post_count(self) -> float:
        return float(self._post_count)

    @property
    def average_popularity(self) -> float:
        return self._cumulative_popularity / self._post_count if self._post_count > 0 else 0.0

    @staticmethod
    def empty() -> AuthorAggregate:
        return AuthorAggregate()


@dataclass(frozen=True)
class TrainingRow:
    post_id: str
    features: list[float]
    label: float
    created_utc: float = 0.0


@dataclass(frozen=True)
class ScanResult:
    sampled_posts: list[SubmissionRecord]
    author_aggregates: dict[str, AuthorAggregate]
    scan_stats: dict[str, int]


@dataclass(frozen=True)
class TrainingDataset:
    rows: list[TrainingRow]
    feature_stats: dict[str, Any]


@dataclass(frozen=True)
class DatasetSplit:
    train_rows: list[TrainingRow]
    validation_rows: list[TrainingRow]


@dataclass(frozen=True)
class Metrics:
    train_rmse: float
    validation_rmse: float
    train_mae: float
    validation_mae: float
    train_ndcg_k: float
    validation_ndcg_k: float


@dataclass(frozen=True)
class GradientBoostedModel:
    model_dump: dict[str, Any]
    metrics: Metrics


@dataclass(frozen=True)
class TrainingRunResult:
    output_path: Path
    trained_at: str
    metrics: Metrics
    train_rows: int
    validation_rows: int


@dataclass(frozen=True)
class InteractionScanResult:
    interactions: dict[str, dict[str, list[float]]]
    stats: dict[str, int]
