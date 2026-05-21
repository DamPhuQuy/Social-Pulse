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
    sample_size: int = 0
    scan_limit_posts: int = 0
    scan_limit_comments: int = 0
    min_content_length: int = 20
    max_content_length: int = 20000
    exclude_nsfw: bool = True
    dedupe_posts: bool = True
    filter_bots: bool = True
    min_distinct_token_count: int = 3
    min_alpha_char_count: int = 12
    max_url_count: int = 8
    n_estimators: int = 1200
    max_depth: int = 8
    min_samples_leaf: int = 32
    min_child_weight: float = 8.0
    learning_rate: float = 0.05
    subsample: float = 0.85
    colsample_bytree: float = 0.80
    reg_lambda: float = 1.5
    reg_alpha: float = 0.05
    max_bin: int = 256
    early_stopping_rounds: int = 50
    validation_ratio: float = 0.2
    test_ratio: float = 0.1
    negative_samples_per_post: int = 4
    trainer_backend: str = "xgboost"
    device: str = "cuda"
    n_jobs: int = 0
    allow_cpu_fallback: bool = True
    plots_output_dir: Path | None = None
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

        def str_val(key: str, default: str) -> str:
            v = values.get(key)
            return v if v else default

        def int_val(key: str, default: int) -> int:
            v = values.get(key)
            return int(v) if v else default

        def float_val(key: str, default: float) -> float:
            v = values.get(key)
            return float(v) if v else default

        def bool_val(key: str, default: bool) -> bool:
            v = values.get(key)
            if v is None:
                return default
            return v.strip().lower() in {"1", "true", "yes", "on"}

        return TrainingArguments(
            submissions_path=required_path("submissions"),
            comments_path=optional_path("comments"),
            output_path=required_path("output"),
            metrics_output_path=optional_path("metrics-output"),
            sample_size=int_val("sample-size", 0),
            scan_limit_posts=int_val("scan-limit-posts", 0),
            scan_limit_comments=int_val("scan-limit-comments", 0),
            min_content_length=int_val("min-content-length", 20),
            max_content_length=int_val("max-content-length", 20000),
            exclude_nsfw=bool_val("exclude-nsfw", True),
            dedupe_posts=bool_val("dedupe-posts", True),
            filter_bots=bool_val("filter-bots", True),
            min_distinct_token_count=int_val("min-distinct-token-count", 3),
            min_alpha_char_count=int_val("min-alpha-char-count", 12),
            max_url_count=int_val("max-url-count", 8),
            n_estimators=int_val("n-estimators", 1200),
            max_depth=int_val("max-depth", 8),
            min_samples_leaf=int_val("min-samples-leaf", 32),
            min_child_weight=float_val("min-child-weight", 8.0),
            learning_rate=float_val("learning-rate", 0.05),
            subsample=float_val("subsample", 0.85),
            colsample_bytree=float_val("colsample-bytree", 0.80),
            reg_lambda=float_val("reg-lambda", 1.5),
            reg_alpha=float_val("reg-alpha", 0.05),
            max_bin=int_val("max-bin", 256),
            early_stopping_rounds=int_val("early-stopping-rounds", 50),
            validation_ratio=float_val("validation-ratio", 0.2),
            test_ratio=float_val("test-ratio", 0.1),
            negative_samples_per_post=int_val("negative-samples-per-post", 4),
            trainer_backend=str_val("trainer-backend", "xgboost"),
            device=str_val("device", "cuda"),
            n_jobs=int_val("n-jobs", 0),
            allow_cpu_fallback=bool_val("allow-cpu-fallback", True),
            plots_output_dir=optional_path("plots-output-dir"),
            seed=int_val("seed", 42),
        )

    def validate(self) -> None:
        if not self.submissions_path.exists():
            raise ValueError(f"Submissions archive not found: {self.submissions_path}")
        if self.comments_path and not self.comments_path.exists():
            raise ValueError(f"Comments archive not found: {self.comments_path}")
        if self.min_content_length <= 0:
            raise ValueError("min_content_length must be positive.")
        if self.max_content_length < self.min_content_length:
            raise ValueError("max_content_length must be >= min_content_length.")
        if self.min_distinct_token_count <= 0:
            raise ValueError("min_distinct_token_count must be positive.")
        if self.min_alpha_char_count <= 0:
            raise ValueError("min_alpha_char_count must be positive.")
        if self.max_url_count < 0:
            raise ValueError("max_url_count must be non-negative.")
        if any(v <= 0 for v in [self.n_estimators, self.max_depth, self.min_samples_leaf, self.learning_rate]):
            raise ValueError("Training arguments must be positive.")
        if self.min_child_weight <= 0:
            raise ValueError("min_child_weight must be positive.")
        if self.max_bin <= 1:
            raise ValueError("max_bin must be greater than 1.")
        if not 0.0 < self.subsample <= 1.0:
            raise ValueError("subsample must be in (0, 1].")
        if not 0.0 < self.colsample_bytree <= 1.0:
            raise ValueError("colsample_bytree must be in (0, 1].")
        if not 0.05 <= self.validation_ratio < 0.5:
            raise ValueError("validation_ratio must be between 0.05 and 0.5.")
        if not 0.05 <= self.test_ratio < 0.3:
            raise ValueError("test_ratio must be between 0.05 and 0.3.")
        if self.validation_ratio + self.test_ratio >= 0.5:
            raise ValueError("validation_ratio + test_ratio must be less than 0.5.")
        if self.negative_samples_per_post < 0:
            raise ValueError("negative_samples_per_post must be non-negative.")
        if self.trainer_backend not in {"xgboost", "sklearn"}:
            raise ValueError("trainer_backend must be one of: xgboost, sklearn.")
        if self.device not in {"cuda", "cpu"}:
            raise ValueError("device must be either 'cuda' or 'cpu'.")
        if self.n_jobs < 0:
            raise ValueError("n_jobs must be >= 0, where 0 means auto.")
