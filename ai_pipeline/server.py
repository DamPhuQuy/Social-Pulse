"""FastAPI server exposing the tree model ranking service over HTTP."""
from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI
from pydantic import AliasChoices, BaseModel, ConfigDict, Field

from ai_pipeline.inference import FeatureVectorizer, RankingProperties, RankingService
from ai_pipeline.inference.vectorizer import (
    AuthorFeatures,
    InteractionFeatures,
    PostFeatures,
    RankingFeatures,
)

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

app = FastAPI(title="AI Pipeline Ranking Service")

# --- Request / Response schemas ---


class ApiDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")


class PostFeaturesDto(ApiDto):
    content_length: int | None = Field(
        default=None,
        validation_alias=AliasChoices("content_length", "contentLength"),
        serialization_alias="content_length",
    )
    has_multimedia: bool | None = Field(
        default=None,
        validation_alias=AliasChoices("has_multimedia", "hasMultimedia"),
        serialization_alias="has_multimedia",
    )
    is_share_post: bool | None = Field(
        default=None,
        validation_alias=AliasChoices("is_share_post", "isSharePost"),
        serialization_alias="is_share_post",
    )
    post_age_hours: float | None = Field(
        default=None,
        validation_alias=AliasChoices("post_age_hours", "postAgeHours"),
        serialization_alias="post_age_hours",
    )
    hot_score: float | None = Field(
        default=None,
        validation_alias=AliasChoices("hot_score", "hotScore"),
        serialization_alias="hot_score",
    )
    upvote_ratio: float | None = Field(
        default=None,
        validation_alias=AliasChoices("upvote_ratio", "upvoteRatio"),
        serialization_alias="upvote_ratio",
    )
    upvote_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("upvote_count", "upvoteCount"),
        serialization_alias="upvote_count",
    )
    downvote_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("downvote_count", "downvoteCount"),
        serialization_alias="downvote_count",
    )
    comment_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("comment_count", "commentCount"),
        serialization_alias="comment_count",
    )
    share_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("share_count", "shareCount"),
        serialization_alias="share_count",
    )
    view_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("view_count", "viewCount"),
        serialization_alias="view_count",
    )
    popularity: float | None = None


class AuthorFeaturesDto(ApiDto):
    seniority_years: float | None = Field(
        default=None,
        validation_alias=AliasChoices("seniority_years", "seniorityYears"),
        serialization_alias="seniority_years",
    )
    post_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("post_count", "postCount"),
        serialization_alias="post_count",
    )
    average_popularity: float | None = Field(
        default=None,
        validation_alias=AliasChoices("average_popularity", "averagePopularity"),
        serialization_alias="average_popularity",
    )


class InteractionFeaturesDto(ApiDto):
    interaction_count_7d: int | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_count_7d", "interaction_count7d", "interactionCount7d"),
        serialization_alias="interaction_count_7d",
    )
    interaction_count_30d: int | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_count_30d", "interaction_count30d", "interactionCount30d"),
        serialization_alias="interaction_count_30d",
    )
    hours_since_last_interaction: float | None = Field(
        default=None,
        validation_alias=AliasChoices("hours_since_last_interaction", "hoursSinceLastInteraction"),
        serialization_alias="hours_since_last_interaction",
    )
    affinity_score: float | None = Field(
        default=None,
        validation_alias=AliasChoices("affinity_score", "affinityScore"),
        serialization_alias="affinity_score",
    )


class RankingFeaturesDto(ApiDto):
    post_id: int = Field(
        validation_alias=AliasChoices("post_id", "postId"),
        serialization_alias="post_id",
    )
    post_features: PostFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("post_features", "postFeatures"),
        serialization_alias="post_features",
    )
    author_features: AuthorFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("author_features", "authorFeatures"),
        serialization_alias="author_features",
    )
    interaction_features: InteractionFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_features", "interactionFeatures"),
        serialization_alias="interaction_features",
    )


class RankingRequestDto(ApiDto):
    feature_schema_version: str = Field(
        default="v1",
        validation_alias=AliasChoices("feature_schema_version", "featureSchemaVersion"),
        serialization_alias="feature_schema_version",
    )
    features: list[RankingFeaturesDto]


class RankingResponseDto(ApiDto):
    post_id: int = Field(serialization_alias="post_id")
    score: float
    feature_schema_version: str = Field(serialization_alias="feature_schema_version")


# --- Service initialization ---

import os


def _env(name: str, default: str, legacy_name: str | None = None) -> str:
    value = os.getenv(name)
    if value is not None:
        return value
    if legacy_name:
        legacy_value = os.getenv(legacy_name)
        if legacy_value is not None:
            return legacy_value
    return default


_props = RankingProperties(
    enabled=_env("AI_PIPELINE_ENABLED", "true", "AI_ENABLED").lower() == "true",
    model_location=_env("AI_PIPELINE_MODEL_LOCATION", "ai_pipeline/model/model.json", "AI_MODEL_LOCATION"),
    feature_schema_version=_env("AI_PIPELINE_FEATURE_SCHEMA_VERSION", "v1", "AI_FEATURE_SCHEMA_VERSION"),
    inference_device=_env("AI_PIPELINE_INFERENCE_DEVICE", "cpu"),
)
_vectorizer = FeatureVectorizer()
_service = RankingService(_props, _vectorizer)


# --- Endpoints ---


@app.post("/api/ranking/predict", response_model=list[RankingResponseDto])
def predict(request: RankingRequestDto) -> list[RankingResponseDto]:
    features = [
        RankingFeatures(
            post_id=f.post_id,
            post_features=PostFeatures(**f.post_features.model_dump()) if f.post_features else None,
            author_features=AuthorFeatures(**f.author_features.model_dump()) if f.author_features else None,
            interaction_features=InteractionFeatures(**f.interaction_features.model_dump()) if f.interaction_features else None,
        )
        for f in request.features
    ]
    results = _service.predict_scores(request.feature_schema_version, features)
    return [
        RankingResponseDto(post_id=r.post_id, score=r.score, feature_schema_version=r.feature_schema_version)
        for r in results
    ]


@app.get("/health")
def health() -> dict[str, str]:
    return {"status": "ok"}
