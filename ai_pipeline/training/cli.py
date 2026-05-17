"""CLI entry point for training pipeline."""
from __future__ import annotations

import sys

from . import json_support as js
from .arguments import TrainingArguments
from .pipeline import PushshiftTrainingPipeline


def main(args: list[str] | None = None) -> None:
    arguments = TrainingArguments.parse(args if args is not None else sys.argv[1:])
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
