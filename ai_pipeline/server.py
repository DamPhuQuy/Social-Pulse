"""FastAPI server exposing the LightGBM ranking service over HTTP."""
from __future__ import annotations

import logging
from typing import Any

from fastapi import FastAPI
from pydantic import BaseModel

from ai_pipeline.inference import LightGbmFeatureVectorizer, LightGbmProperties, LightGbmRankingService
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


class PostFeaturesDto(BaseModel):
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


class AuthorFeaturesDto(BaseModel):
    seniority_years: float | None = None
    post_count: int | None = None
    average_popularity: float | None = None


class InteractionFeaturesDto(BaseModel):
    interaction_count_7d: int | None = None
    interaction_count_30d: int | None = None
    hours_since_last_interaction: float | None = None
    affinity_score: float | None = None


class RankingFeaturesDto(BaseModel):
    post_id: int
    post_features: PostFeaturesDto | None = None
    author_features: AuthorFeaturesDto | None = None
    interaction_features: InteractionFeaturesDto | None = None


class RankingRequestDto(BaseModel):
    feature_schema_version: str = "v1"
    features: list[RankingFeaturesDto]


class RankingResponseDto(BaseModel):
    post_id: int
    score: float
    feature_schema_version: str


# --- Service initialization ---

import os

_props = LightGbmProperties(
    enabled=os.getenv("AI_ENABLED", "true").lower() == "true",
    model_location=os.getenv("AI_MODEL_LOCATION", "ai_pipeline/model/model.json"),
    feature_schema_version=os.getenv("AI_FEATURE_SCHEMA_VERSION", "v1"),
)
_vectorizer = LightGbmFeatureVectorizer()
_service = LightGbmRankingService(_props, _vectorizer)


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
