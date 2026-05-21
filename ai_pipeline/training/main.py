"""CLI entry point for training pipeline."""
from __future__ import annotations

import sys
from pathlib import Path

from . import json_support as js
from .arguments import TrainingArguments
from .pipeline import PushshiftTrainingPipeline

_BASE = Path(__file__).resolve().parent.parent
_SUBMISSIONS = _BASE / "data" / "RS_2019-04.zst"
_COMMENTS = _BASE / "data" / "RC_2019-04.zst"
_OUTPUT = _BASE / "model" / "model.json"
_METRICS_OUTPUT = _BASE / "model" / "metrics.json"
_PLOTS_OUTPUT = _BASE / "model" / "plots"


def main(args: list[str] | None = None) -> None:
    argv = args if args is not None else sys.argv[1:]
    if not argv:
        argv = [
            "--submissions", str(_SUBMISSIONS),
            "--comments", str(_COMMENTS),
            "--output", str(_OUTPUT),
            "--metrics-output", str(_METRICS_OUTPUT),
            "--plots-output-dir", str(_PLOTS_OUTPUT),
        ]
    arguments = TrainingArguments.parse(argv)
    result = PushshiftTrainingPipeline().run(arguments)

    output = {
        "output": str(result.output_path),
        "trained_at": result.trained_at,
        "model_backend": result.model_backend,
        "train_rmse": js.round6(result.metrics.train_rmse),
        "validation_rmse": js.round6(result.metrics.validation_rmse),
        "test_rmse": js.round6(result.metrics.test_rmse),
        "train_mae": js.round6(result.metrics.train_mae),
        "validation_mae": js.round6(result.metrics.validation_mae),
        "test_mae": js.round6(result.metrics.test_mae),
        "validation_ndcg_k": js.round6(result.metrics.validation_ndcg_k),
        "test_ndcg_k": js.round6(result.metrics.test_ndcg_k),
        "validation_r2": js.round6(result.metrics.validation_r2),
        "test_r2": js.round6(result.metrics.test_r2),
        "rows": {"train": result.train_rows, "validation": result.validation_rows, "test": result.test_rows},
        "evaluation_warnings": result.evaluation_warnings,
    }
    print(js.to_pretty_json(output))


if __name__ == "__main__":
    main()
