"""Ranking service: loads model artifact and scores feature vectors."""
from __future__ import annotations

import json
import logging
import threading
from dataclasses import dataclass
from pathlib import Path
from typing import Any

from ai_pipeline.shared.model import LightGbmModel, parse_artifact, parse_model
from ai_pipeline.shared.schema import LightGbmFeatureSchema
from ai_pipeline.shared.scorer import LightGbmModelScorer
from .vectorizer import LightGbmFeatureVectorizer, RankingFeatures

logger = logging.getLogger(__name__)


@dataclass
class LightGbmProperties:
    enabled: bool = False
    model_location: str = "ai/lightgbm-ranking-model.json"
    feature_schema_version: str = LightGbmFeatureSchema.DEFAULT_SCHEMA_VERSION


@dataclass
class RankingResponse:
    post_id: int
    score: float
    feature_schema_version: str


class LightGbmRankingService:
    def __init__(
        self,
        properties: LightGbmProperties,
        vectorizer: LightGbmFeatureVectorizer,
    ):
        self._properties = properties
        self._vectorizer = vectorizer
        self._scorer: LightGbmModelScorer | None = None
        self._lock = threading.Lock()

    def predict_scores(
        self, feature_schema_version: str, features: list[RankingFeatures]
    ) -> list[RankingResponse]:
        if not self._properties.enabled or not features:
            return []
        if self._properties.feature_schema_version != feature_schema_version:
            logger.warning("Schema mismatch: expected=%s, actual=%s",
                           self._properties.feature_schema_version, feature_schema_version)
            return []

        scorer = self._get_or_load_scorer()
        if scorer is None:
            return []

        return [
            RankingResponse(
                post_id=f.post_id,
                score=scorer.score(self._vectorizer.to_feature_map(f)),
                feature_schema_version=feature_schema_version,
            )
            for f in features
        ]

    def _get_or_load_scorer(self) -> LightGbmModelScorer | None:
        if self._scorer is not None:
            return self._scorer
        with self._lock:
            if self._scorer is not None:
                return self._scorer
            self._scorer = self._load_scorer()
            return self._scorer

    def _load_scorer(self) -> LightGbmModelScorer | None:
        path = Path(self._properties.model_location)
        if not path.exists():
            logger.warning("Model not found at %s", path)
            return None

        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as e:
            logger.warning("Failed to read model: %s", e)
            return None

        try:
            model, schema_ver, dataset, trained_at = self._read_artifact(data)
        except (ValueError, KeyError) as e:
            logger.warning("Failed to parse model: %s", e)
            return None

        if schema_ver and schema_ver != self._properties.feature_schema_version:
            logger.warning("Artifact schema mismatch: expected=%s, actual=%s",
                           self._properties.feature_schema_version, schema_ver)
            return None

        logger.info("Loaded model from %s: %d trees, objective=%s, schema=%s, dataset=%s, trainedAt=%s",
                    path, len(model.tree_info), model.objective_name, schema_ver, dataset, trained_at)
        return LightGbmModelScorer(model)

    def _read_artifact(self, data: dict) -> tuple[LightGbmModel, str | None, str | None, str | None]:
        if "tree_info" in data:
            model = parse_model(data)
            return model, self._properties.feature_schema_version, None, None

        if "model_dump" in data:
            artifact = parse_artifact(data)
            if artifact.model_dump is None:
                raise ValueError("Artifact missing model_dump")
            return artifact.model_dump, artifact.feature_schema_version, artifact.training_dataset, artifact.trained_at

        raise ValueError("Unsupported artifact format")
