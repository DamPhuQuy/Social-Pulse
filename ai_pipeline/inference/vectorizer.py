"""Feature vectorizer: converts structured features into the model's feature map."""
from __future__ import annotations

import math
from dataclasses import dataclass
from typing import Any

from ai_pipeline.shared.schema import RankingFeatureSchema

_LOG_TRANSFORM_FEATURES = set(RankingFeatureSchema.LOG_TRANSFORM_FEATURES)


@dataclass
class PostFeatures:
    content_length: int | None = None
    has_multimedia: bool | None = None
    is_share_post: bool | None = None
    post_age_hours: float | None = None


@dataclass
class AuthorFeatures:
    seniority_years: float | None = None
    post_count: int | None = None
    average_popularity: float | None = None


@dataclass
class InteractionFeatures:
    interaction_count_7d: int | None = None
    interaction_count_30d: int | None = None
    hours_since_last_interaction: float | None = None
    affinity_score: float | None = None


@dataclass
class RankingFeatures:
    post_id: int
    post_features: PostFeatures | None = None
    author_features: AuthorFeatures | None = None
    interaction_features: InteractionFeatures | None = None


class FeatureVectorizer:
    FEATURE_ORDER = RankingFeatureSchema.FEATURE_ORDER
    _DEFAULT_HOURS = RankingFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS

    def __init__(self):
        self._preprocessing: dict[str, Any] = {}

    def set_preprocessing(self, preprocessing: dict[str, Any] | None) -> None:
        self._preprocessing = preprocessing or {}

    def to_feature_map(self, features: RankingFeatures) -> dict[str, float]:
        post_features = features.post_features
        author_features = features.author_features
        interaction_features = features.interaction_features

        values: dict[str, float] = {}
        values["content_length"] = _safe_int(post_features.content_length if post_features else None)
        values["has_multimedia"] = _to_binary(post_features.has_multimedia if post_features else None)
        values["is_share_post"] = _to_binary(post_features.is_share_post if post_features else None)
        values["post_age_hours"] = _safe(post_features.post_age_hours if post_features else None)

        values["author_seniority"] = _safe(author_features.seniority_years if author_features else None)
        values["author_post_count"] = _safe_int(author_features.post_count if author_features else None)
        values["author_engagement_rate"] = _safe(author_features.average_popularity if author_features else None)

        values["interaction_count_7d"] = _safe_int(interaction_features.interaction_count_7d if interaction_features else None)
        values["interaction_count_30d"] = _safe_int(interaction_features.interaction_count_30d if interaction_features else None)
        values["hours_since_last_interaction"] = _safe(
            interaction_features.hours_since_last_interaction if interaction_features else None,
            self._DEFAULT_HOURS,
        )
        values["affinity_score"] = _safe(interaction_features.affinity_score if interaction_features else None)

        self._apply_preprocessing(values)
        return values

    def to_ordered_vector(self, features: RankingFeatures) -> list[float]:
        feature_map = self.to_feature_map(features)
        return [feature_map[name] for name in self.FEATURE_ORDER]

    def _apply_preprocessing(self, values: dict[str, float]) -> None:
        cap_values = self._preprocessing.get("cap_values", {})
        for name, cap in cap_values.items():
            if name in values:
                values[name] = min(values[name], float(cap))

        log_features = self._preprocessing.get("log_transform_features") or list(_LOG_TRANSFORM_FEATURES)
        for name in log_features:
            if name in values:
                values[name] = math.log1p(max(values[name], 0.0))


def _to_binary(value: bool | None) -> float:
    return 1.0 if value else 0.0


def _safe(value: float | None, default: float = 0.0) -> float:
    return float(value) if value is not None else default


def _safe_int(value: int | None) -> float:
    return float(value) if value is not None else 0.0
