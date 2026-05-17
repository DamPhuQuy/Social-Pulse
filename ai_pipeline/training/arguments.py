"""Training argument parsing and validation."""
from __future__ import annotations

from dataclasses import dataclass
from pathlib import Path


@dataclass
class TrainingArguments:
    submissions_path: Path
    comments_path: Path | None
    output_path: Path
    metrics_output_path: Path | None
    sample_size: int = 100000
    scan_limit_posts: int = 0
    scan_limit_comments: int = 0
    min_content_length: int = 20
    n_estimators: int = 16
    max_depth: int = 3
    min_samples_leaf: int = 64
    max_thresholds: int = 16
    learning_rate: float = 0.18
    seed: int = 42

    @staticmethod
    def parse(args: list[str]) -> TrainingArguments:
        values: dict[str, str] = {}
        i = 0
        while i < len(args):
            arg = args[i]
            if not arg.startswith("--"):
                raise ValueError(f"Unsupported argument: {arg}")
            if i + 1 >= len(args):
                raise ValueError(f"Missing value for {arg}")
            values[arg[2:]] = args[i + 1]
            i += 2

        def required_path(key: str) -> Path:
            v = values.get(key)
            if not v:
                raise ValueError(f"Missing required argument --{key}")
            return Path(v)

        def optional_path(key: str) -> Path | None:
            v = values.get(key)
            return Path(v) if v else None

        def int_val(key: str, default: int) -> int:
            v = values.get(key)
            return int(v) if v else default

        def float_val(key: str, default: float) -> float:
            v = values.get(key)
            return float(v) if v else default

        return TrainingArguments(
            submissions_path=required_path("submissions"),
            comments_path=optional_path("comments"),
            output_path=required_path("output"),
            metrics_output_path=optional_path("metrics-output"),
            sample_size=int_val("sample-size", 12000),
            scan_limit_posts=int_val("scan-limit-posts", 180000),
            scan_limit_comments=int_val("scan-limit-comments", 300000),
            min_content_length=int_val("min-content-length", 20),
            n_estimators=int_val("n-estimators", 16),
            max_depth=int_val("max-depth", 3),
            min_samples_leaf=int_val("min-samples-leaf", 64),
            max_thresholds=int_val("max-thresholds", 16),
            learning_rate=float_val("learning-rate", 0.18),
            seed=int_val("seed", 42),
        )

    def validate(self) -> None:
        if not self.submissions_path.exists():
            raise ValueError(f"Submissions archive not found: {self.submissions_path}")
        if self.comments_path and not self.comments_path.exists():
            raise ValueError(f"Comments archive not found: {self.comments_path}")
        if any(v <= 0 for v in [
            self.sample_size, self.n_estimators, self.max_depth,
            self.min_samples_leaf, self.max_thresholds, self.learning_rate,
        ]):
            raise ValueError("Training arguments must be positive.")
