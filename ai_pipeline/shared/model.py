"""Model data types and JSON deserialization."""
from __future__ import annotations

from dataclasses import dataclass, field
from typing import Any


@dataclass
class TreeNode:
    split_feature: int | None = None
    threshold: float | None = None
    decision_type: str | None = None
    default_left: bool | None = None
    left_child: TreeNode | None = None
    right_child: TreeNode | None = None
    leaf_value: float | None = None

    @property
    def is_leaf(self) -> bool:
        return self.leaf_value is not None or (self.left_child is None and self.right_child is None)


@dataclass
class TreeInfo:
    shrinkage: float = 1.0
    tree_structure: TreeNode | None = None


@dataclass
class LightGbmModel:
    feature_names: list[str] = field(default_factory=list)
    tree_info: list[TreeInfo] = field(default_factory=list)
    average_output: bool = False
    objective: Any = None

    @property
    def objective_name(self) -> str:
        if self.objective is None:
            return ""
        if isinstance(self.objective, str):
            return self.objective
        if isinstance(self.objective, dict):
            return self.objective.get("name", "")
        return ""

    def get_feature_name(self, feature_index: int) -> str:
        if feature_index < 0 or feature_index >= len(self.feature_names):
            raise ValueError(f"Feature index out of bounds: {feature_index}")
        return self.feature_names[feature_index]


@dataclass
class LightGbmModelArtifact:
    artifact_version: str = "1"
    feature_schema_version: str | None = None
    training_dataset: str | None = None
    trained_at: str | None = None
    label_strategy: str | None = None
    model_dump: LightGbmModel | None = None


def _parse_tree_node(data: dict | None) -> TreeNode | None:
    if data is None:
        return None
    return TreeNode(
        split_feature=data.get("split_feature"),
        threshold=data.get("threshold"),
        decision_type=data.get("decision_type"),
        default_left=data.get("default_left"),
        left_child=_parse_tree_node(data.get("left_child")),
        right_child=_parse_tree_node(data.get("right_child")),
        leaf_value=data.get("leaf_value"),
    )


def parse_model(data: dict) -> LightGbmModel:
    tree_info_list = []
    for ti in data.get("tree_info", []):
        tree_info_list.append(TreeInfo(
            shrinkage=ti.get("shrinkage", 1.0),
            tree_structure=_parse_tree_node(ti.get("tree_structure")),
        ))
    return LightGbmModel(
        feature_names=data.get("feature_names", []),
        tree_info=tree_info_list,
        average_output=data.get("average_output", False),
        objective=data.get("objective"),
    )


def parse_artifact(data: dict) -> LightGbmModelArtifact:
    model_dump_data = data.get("model_dump")
    model = parse_model(model_dump_data) if model_dump_data else None
    return LightGbmModelArtifact(
        artifact_version=data.get("artifact_version", "1"),
        feature_schema_version=data.get("feature_schema_version"),
        training_dataset=data.get("training_dataset"),
        trained_at=data.get("trained_at"),
        label_strategy=data.get("label_strategy"),
        model_dump=model,
    )
