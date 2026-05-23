from __future__ import annotations

import os

from ai_pipeline.inference import FeatureVectorizer, RankingProperties, RankingService


def _env(name: str, default: str, *, legacy_name: str | None = None) -> str:
    value = os.getenv(name)
    if value is not None:
        return value
    if legacy_name:
        legacy_value = os.getenv(legacy_name)
        if legacy_value is not None:
            return legacy_value
    return default

class RankingServiceConfig:

    # Environment variable names
    _ENV_ENABLED = "AI_PIPELINE_ENABLED"
    _ENV_MODEL_LOCATION = "AI_PIPELINE_MODEL_LOCATION"
    _ENV_FEATURE_SCHEMA = "AI_PIPELINE_FEATURE_SCHEMA_VERSION"
    _ENV_INFERENCE_DEVICE = "AI_PIPELINE_INFERENCE_DEVICE"

    # Legacy aliases (backward-compatible)
    _LEGACY_ENABLED = "AI_ENABLED"
    _LEGACY_MODEL_LOCATION = "AI_MODEL_LOCATION"
    _LEGACY_FEATURE_SCHEMA = "AI_FEATURE_SCHEMA_VERSION"

    # Defaults
    _DEFAULT_MODEL_LOCATION = "ai_pipeline/model/model.json"
    _DEFAULT_FEATURE_SCHEMA = "v2"
    _DEFAULT_INFERENCE_DEVICE = "cpu"

    def build(self) -> RankingService:
        properties = self._ranking_properties()
        vectorizer = self._feature_vectorizer()
        return RankingService(properties, vectorizer)

    # --- private factory methods (analogous to @Bean methods) ---------------

    def _ranking_properties(self) -> RankingProperties:
        enabled_str = _env(
            self._ENV_ENABLED, "true",
            legacy_name=self._LEGACY_ENABLED,
        )
        return RankingProperties(
            enabled=enabled_str.lower() == "true",
            model_location=_env(
                self._ENV_MODEL_LOCATION, self._DEFAULT_MODEL_LOCATION,
                legacy_name=self._LEGACY_MODEL_LOCATION,
            ),
            feature_schema_version=_env(
                self._ENV_FEATURE_SCHEMA, self._DEFAULT_FEATURE_SCHEMA,
                legacy_name=self._LEGACY_FEATURE_SCHEMA,
            ),
            inference_device=_env(
                self._ENV_INFERENCE_DEVICE, self._DEFAULT_INFERENCE_DEVICE,
            ),
        )

    @staticmethod
    def _feature_vectorizer() -> FeatureVectorizer:
        return FeatureVectorizer()
