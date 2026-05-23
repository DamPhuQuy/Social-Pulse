"""Ranking service: loads model artifact and scores feature vectors."""
from __future__ import annotations

import json
import logging
import threading
from dataclasses import dataclass
from pathlib import Path

from ai_pipeline.shared.model import TreeModel, parse_artifact, parse_model
from ai_pipeline.shared.schema import RankingFeatureSchema
from ai_pipeline.shared.scorer import TreeModelScorer
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
        self._scorer: TreeModelScorer | None = None
        self._lgb_booster = None
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

        scorer = self._get_or_load_scorer()
        if scorer is None and self._lgb_booster is None:
            return []

        if self._lgb_booster is not None:
            import numpy as np

            matrix = np.array(
                [self._vectorizer.to_ordered_vector(feature) for feature in features],
                dtype=np.float32,
            )
            scores = self._lgb_booster.predict(matrix)
            return [
                RankingResponse(
                    post_id=feature.post_id,
                    score=float(score),
                    feature_schema_version=feature_schema_version,
                )
                for feature, score in zip(features, scores)
            ]

        return [
            RankingResponse(
                post_id=feature.post_id,
                score=scorer.score(self._vectorizer.to_feature_map(feature)),
                feature_schema_version=feature_schema_version,
            )
            for feature in features
        ]

    def _get_or_load_scorer(self) -> TreeModelScorer | None:
        if self._scorer is not None or self._lgb_booster is not None:
            return self._scorer
        with self._lock:
            if self._scorer is not None or self._lgb_booster is not None:
                return self._scorer
            self._scorer = self._load_scorer()
            return self._scorer

    def _load_scorer(self) -> TreeModelScorer | None:
        path = Path(self._properties.model_location)
        if not path.exists():
            logger.warning("Model not found at %s", path)
            return None

        try:
            data = json.loads(path.read_text(encoding="utf-8"))
        except (OSError, json.JSONDecodeError) as exc:
            logger.warning("Failed to read model: %s", exc)
            return None

        try:
            model, schema_ver, dataset, trained_at, preprocessing, model_backend, model_file = self._read_artifact(data)
        except (ValueError, KeyError) as exc:
            logger.warning("Failed to parse model: %s", exc)
            return None

        self._vectorizer.set_preprocessing(preprocessing)

        if schema_ver and schema_ver != self._properties.feature_schema_version:
            logger.warning(
                "Artifact schema mismatch: expected=%s, actual=%s",
                self._properties.feature_schema_version,
                schema_ver,
            )
            return None

        if model_backend == "lightgbm":
            if not model_file:
                logger.warning("Artifact declares lightgbm backend but is missing model_file")
                return None
            model_path = path.parent / model_file
            if not model_path.exists():
                logger.warning("LightGBM model sidecar not found at %s", model_path)
                return None
            try:
                import lightgbm as lgb

                booster = lgb.Booster(model_file=str(model_path))
            except Exception as exc:
                logger.warning("Failed to load LightGBM booster: %s", exc)
                return None
            self._lgb_booster = booster
            self._scorer = None
            logger.info(
                "Loaded lightgbm model from %s: schema=%s, dataset=%s, trainedAt=%s",
                model_path,
                schema_ver,
                dataset,
                trained_at,
            )
            return None

        logger.info(
            "Loaded model from %s: %d trees, objective=%s, schema=%s, dataset=%s, trainedAt=%s",
            path,
            len(model.tree_info),
            model.objective_name,
            schema_ver,
            dataset,
            trained_at,
        )
        return TreeModelScorer(model)

    def _read_artifact(
        self,
        data: dict,
    ) -> tuple[TreeModel, str | None, str | None, str | None, dict | None, str | None, str | None]:
        if "tree_info" in data:
            model = parse_model(data)
            return model, self._properties.feature_schema_version, None, None, None, "custom_tree_json", None

        if "model_dump" in data:
            artifact = parse_artifact(data)
            if artifact.model_dump is None:
                raise ValueError("Artifact missing model_dump")
            return (
                artifact.model_dump,
                artifact.feature_schema_version,
                artifact.training_dataset,
                artifact.trained_at,
                artifact.preprocessing,
                artifact.model_backend,
                artifact.model_file,
            )

        if data.get("model_backend") == "lightgbm":
            artifact = parse_artifact(data)
            return (
                TreeModel(),
                artifact.feature_schema_version,
                artifact.training_dataset,
                artifact.trained_at,
                artifact.preprocessing,
                artifact.model_backend,
                artifact.model_file,
            )

        raise ValueError("Unsupported artifact format")
