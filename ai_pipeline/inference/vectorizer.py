"""Feature vectorizer: converts structured features into the model's feature map."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any

from ..shared.schema import LightGbmFeatureSchema


@dataclass
class PostFeatures:
    content_length: int | None = None
    has_multimedia: bool | None = None
    is_share_post: bool | None = None
    post_age_hours: float | None = None
    hot_score: float | None = None
    upvote_ratio: float | None = None
    upvote_count: int | None = None
    downvote_count: int | None = None
    comment_count: int | None = None
    share_count: int | None = None
    view_count: int | None = None
    popularity: float | None = None


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


class LightGbmFeatureVectorizer:
    FEATURE_ORDER = LightGbmFeatureSchema.FEATURE_ORDER
    _DEFAULT = LightGbmFeatureSchema.DEFAULT_NUMERIC_VALUE
    _DEFAULT_RATIO = LightGbmFeatureSchema.DEFAULT_UPVOTE_RATIO
    _DEFAULT_HOURS = LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS

    def to_feature_map(self, features: RankingFeatures) -> dict[str, float]:
        pf = features.post_features
        af = features.author_features
        inf = features.interaction_features

        v: dict[str, float] = {}
        v["content_length"] = _safe_int(pf.content_length if pf else None)
        v["has_multimedia"] = _to_binary(pf.has_multimedia if pf else None)
        v["is_share_post"] = _to_binary(pf.is_share_post if pf else None)
        v["post_age_hours"] = _safe(pf.post_age_hours if pf else None)
        v["hot_score"] = _safe(pf.hot_score if pf else None)
        v["upvote_ratio"] = _safe(pf.upvote_ratio if pf else None, self._DEFAULT_RATIO)

        v["author_seniority"] = _safe(af.seniority_years if af else None)
        v["author_post_count"] = _safe_int(af.post_count if af else None)
        v["author_engagement_rate"] = _safe(af.average_popularity if af else None)

        v["interaction_count_7d"] = _safe_int(inf.interaction_count_7d if inf else None)
        v["interaction_count_30d"] = _safe_int(inf.interaction_count_30d if inf else None)
        v["hours_since_last_interaction"] = _safe(inf.hours_since_last_interaction if inf else None, self._DEFAULT_HOURS)
        v["affinity_score"] = _safe(inf.affinity_score if inf else None)

        up = _safe_int(pf.upvote_count if pf else None)
        down = _safe_int(pf.downvote_count if pf else None)
        cmt = _safe_int(pf.comment_count if pf else None)
        share = _safe_int(pf.share_count if pf else None)
        view = _safe_int(pf.view_count if pf else None)

        v["upvote_count"] = up
        v["downvote_count"] = down
        v["comment_count"] = cmt
        v["share_count"] = share
        v["view_count"] = view
        v["popularity"] = _safe(pf.popularity if pf else None, up + cmt + share)
        return v


def _to_binary(value: bool | None) -> float:
    return 1.0 if value else 0.0


def _safe(value: float | None, default: float = 0.0) -> float:
    return float(value) if value is not None else default


def _safe_int(value: int | None) -> float:
    return float(value) if value is not None else 0.0
