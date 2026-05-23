from __future__ import annotations

import logging

from fastapi import APIRouter

from ai_pipeline.inference import RankingService
from ai_pipeline.inference.vectorizer import (
    AuthorFeatures,
    InteractionFeatures,
    PostFeatures,
    RankingFeatures,
)

from .dto import (
    RankingFeaturesDto,
    RankingRequestDto,
    RankingResponseDto,
)

logger = logging.getLogger(__name__)


class RankingController:

    def __init__(self, ranking_service: RankingService) -> None:
        self._service = ranking_service
        self.router = APIRouter(prefix="/api/ranking", tags=["ranking"])
        self._register_routes()

    def _register_routes(self) -> None:
        self.router.add_api_route(
            path="/predict",
            endpoint=self.predict,
            methods=["POST"],
            response_model=list[RankingResponseDto],
            summary="Rank candidate posts",
            description=(
                "Accepts a list of candidate posts with pre-extracted features "
                "and returns a relevance score for each one."
            ),
        )

    def predict(self, request: RankingRequestDto) -> list[RankingResponseDto]:
        """
        POST /api/ranking/predict

        1. Map DTOs → domain RankingFeatures objects.
        2. Delegate scoring to RankingService.
        3. Map domain results → response DTOs.
        """
        domain_features = [self._to_domain(f) for f in request.features]
        results = self._service.predict_scores(request.feature_schema_version, domain_features)

        return [
            RankingResponseDto(
                post_id=result.post_id,
                score=result.score,
                feature_schema_version=result.feature_schema_version,
            )
            for result in results
        ]

    @staticmethod
    def _to_domain(dto: RankingFeaturesDto) -> RankingFeatures:
        return RankingFeatures(
            post_id=dto.post_id,
            post_features=RankingController._map_post_features(dto),
            author_features=RankingController._map_author_features(dto),
            interaction_features=RankingController._map_interaction_features(dto),
        )

    @staticmethod
    def _map_post_features(dto: RankingFeaturesDto) -> PostFeatures | None:
        if dto.post_features is None:
            return None
        return PostFeatures(**dto.post_features.model_dump())

    @staticmethod
    def _map_author_features(dto: RankingFeaturesDto) -> AuthorFeatures | None:
        if dto.author_features is None:
            return None
        return AuthorFeatures(**dto.author_features.model_dump())

    @staticmethod
    def _map_interaction_features(dto: RankingFeaturesDto) -> InteractionFeatures | None:
        if dto.interaction_features is None:
            return None
        return InteractionFeatures(**dto.interaction_features.model_dump())
