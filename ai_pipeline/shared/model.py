"""LightGBM model artifact metadata parsing."""
from __future__ import annotations

from dataclasses import dataclass
from typing import Any


@dataclass(frozen=True)
class RankingModelArtifact:
    artifact_version: str = "1"
    feature_schema_version: str | None = None
    training_dataset: str | None = None
    trained_at: str | None = None
    label_strategy: str | None = None
    model_backend: str | None = None
    model_file: str | None = None
    preprocessing: dict[str, Any] | None = None


def parse_artifact(data: dict[str, Any]) -> RankingModelArtifact:
    return RankingModelArtifact(
        artifact_version=data.get("artifact_version", "1"),
        feature_schema_version=data.get("feature_schema_version"),
        training_dataset=data.get("training_dataset"),
        trained_at=data.get("trained_at"),
        label_strategy=data.get("label_strategy"),
        model_backend=data.get("model_backend"),
        model_file=data.get("model_file"),
        preprocessing=data.get("preprocessing"),
    )
