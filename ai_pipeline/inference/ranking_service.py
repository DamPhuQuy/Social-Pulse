"""Ranking service: loads a LightGBM model artifact and scores feature vectors."""
from __future__ import annotations

import json
import logging
import threading
from dataclasses import dataclass
from pathlib import Path

import numpy as np

from ai_pipeline.shared.model import parse_artifact
from ai_pipeline.shared.schema import RankingFeatureSchema
from .vectorizer import FeatureVectorizer, RankingFeatures

logger = logging.getLogger(__name__)


@dataclass
class RankingProperties:
    enabled: bool = False
    model_location: str = "ai_pipeline/model/model.json"
    feature_schema_version: str = RankingFeatureSchema.DEFAULT_SCHEMA_VERSION
    inference_device: str = "cpu"


@dataclass
class RankingResponse:
    post_id: int
    score: float
    feature_schema_version: str


class RankingService:
    def __init__(
        self,
        properties: RankingProperties,
        vectorizer: FeatureVectorizer,
    ):
        self._properties = properties
        self._vectorizer = vectorizer
        self._booster = None
        self._lock = threading.Lock()

    def predict_scores(
        self,
        feature_schema_version: str,
        features: list[RankingFeatures],
    ) -> list[RankingResponse]:
        if not self._properties.enabled or not features:
            return []
        if self._properties.feature_schema_version != feature_schema_version:
            logger.warning(
                "Schema mismatch: expected=%s, actual=%s",
                self._properties.feature_schema_version,
                feature_schema_version,
            )
            return []

        booster = self._get_or_load_booster()
        if booster is None:
            return []

        matrix = np.array(
            [self._vectorizer.to_ordered_vector(feature) for feature in features],
            dtype=np.float32,
        )
        scores = booster.predict(matrix)
        return [
            RankingResponse(
                post_id=feature.post_id,
                score=float(score),
                feature_schema_version=feature_schema_version,
            )
            for feature, score in zip(features, scores)
        ]

    def status(self) -> dict[str, str | bool]:
        model_location = self._properties.model_location
        if self._properties.enabled and self._booster is None and Path(model_location).exists():
            self._get_or_load_booster()
        return {
            "status": "ok",
            "enabled": self._properties.enabled,
            "feature_schema_version": self._properties.feature_schema_version,
            "model_location": model_location,
            "model_available": Path(model_location).exists(),
            "model_loaded": self._booster is not None,
        }

    def _get_or_load_booster(self):
        if self._booster is not None:
            return self._booster
        with self._lock:
            if self._booster is not None:
                return self._booster
            self._booster = self._load_booster()
            return self._booster

    def _load_booster(self):
        artifact_path = Path(self._properties.model_location)
        if not artifact_path.exists():
            logger.warning("Model artifact not found at %s", artifact_path)
            return None

        try:
            data = json.loads(artifact_path.read_text(encoding="utf-8"))
            artifact = parse_artifact(data)
        except (OSError, json.JSONDecodeError, ValueError, KeyError) as exc:
            logger.warning("Failed to read model artifact: %s", exc)
            return None

        if artifact.model_backend != "lightgbm":
            logger.warning("Unsupported model backend: %s", artifact.model_backend)
            return None
        if artifact.feature_schema_version != self._properties.feature_schema_version:
            logger.warning(
                "Artifact schema mismatch: expected=%s, actual=%s",
                self._properties.feature_schema_version,
                artifact.feature_schema_version,
            )
            return None
        if not artifact.model_file:
            logger.warning("LightGBM artifact is missing model_file")
            return None

        model_path = artifact_path.parent / artifact.model_file
        if not model_path.exists():
            logger.warning("LightGBM model sidecar not found at %s", model_path)
            return None

        try:
            import lightgbm as lgb

            booster = lgb.Booster(model_file=str(model_path))
        except Exception as exc:
            logger.warning("Failed to load LightGBM booster: %s", exc)
            return None

        self._vectorizer.set_preprocessing(artifact.preprocessing)
        logger.info(
            "Loaded LightGBM model from %s: schema=%s, dataset=%s, trainedAt=%s",
            model_path,
            artifact.feature_schema_version,
            artifact.training_dataset,
            artifact.trained_at,
        )
        return booster
