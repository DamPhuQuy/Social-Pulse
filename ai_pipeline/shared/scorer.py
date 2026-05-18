"""Local tree-traversal scorer for LightGBM model artifacts."""
import math

from .model import LightGbmModel, TreeNode
from .schema import LightGbmFeatureSchema


class LightGbmModelScorer:
    def __init__(self, model: LightGbmModel):
        if model is None or not model.tree_info:
            raise ValueError("LightGBM model must contain at least one tree")
        self._model = model

    def score(self, features: dict[str, float]) -> float:
        total = 0.0
        for tree_info in self._model.tree_info:
            tree_score = self._score_node(tree_info.tree_structure, features)
            shrinkage = tree_info.shrinkage if tree_info.shrinkage is not None else 1.0
            total += tree_score * shrinkage

        if self._model.average_output:
            total /= len(self._model.tree_info)
        return total

    def _score_node(self, node: TreeNode | None, features: dict[str, float]) -> float:
        if node is None:
            raise ValueError("Encountered null tree node while scoring")
        if node.is_leaf:
            return node.leaf_value if node.leaf_value is not None else 0.0
        if node.split_feature is None:
            raise ValueError("Non-leaf node is missing split_feature")

        feature_name = self._model.get_feature_name(node.split_feature)
        value, missing = self._resolve_feature(feature_name, features)
        go_left = self._should_go_left(node, value, missing)
        return self._score_node(node.left_child if go_left else node.right_child, features)

    @staticmethod
    def _resolve_feature(name: str, features: dict[str, float]) -> tuple[float, bool]:
        if name not in features:
            return LightGbmFeatureSchema.DEFAULT_NUMERIC_VALUE, False
        value = features[name]
        if value is None or math.isnan(value):
            return float("nan"), True
        return value, False

    @staticmethod
    def _should_go_left(node: TreeNode, value: float, missing: bool) -> bool:
        if missing:
            return bool(node.default_left)
        threshold = node.threshold if node.threshold is not None else 0.0
        dt = node.decision_type
        if dt is None or dt == "" or dt == "<=":
            return value <= threshold
        if dt == "<":
            return value < threshold
        if dt == ">":
            return value > threshold
        if dt == ">=":
            return value >= threshold
        if dt == "==":
            return value == threshold
        raise ValueError(f"Unsupported decision type: {dt}")
