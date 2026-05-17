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


def main(args: list[str] | None = None) -> None:
    argv = args if args is not None else sys.argv[1:]
    if not argv:
        argv = [
            "--submissions", str(_SUBMISSIONS),
            "--comments", str(_COMMENTS),
            "--output", str(_OUTPUT),
            "--metrics-output", str(_METRICS_OUTPUT),
        ]
    arguments = TrainingArguments.parse(argv)
    result = PushshiftTrainingPipeline().run(arguments)

    output = {
        "output": str(result.output_path),
        "trained_at": result.trained_at,
        "train_rmse": js.round6(result.metrics.train_rmse),
        "validation_rmse": js.round6(result.metrics.validation_rmse),
        "rows": {"train": result.train_rows, "validation": result.validation_rows},
    }
    print(js.to_pretty_json(output))


if __name__ == "__main__":
    main()
