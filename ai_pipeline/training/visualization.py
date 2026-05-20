"""Generate lightweight training visualizations for inspection."""
from __future__ import annotations

from pathlib import Path

import numpy as np

from .types import TrainingHistoryPoint, TrainingRow


def generate_training_visualizations(
    output_dir: Path,
    rows: list[TrainingRow],
    history: list[TrainingHistoryPoint],
    feature_importances: dict[str, float],
) -> dict[str, str]:
    output_dir.mkdir(parents=True, exist_ok=True)
    _configure_matplotlib()

    plots: dict[str, str] = {}
    plots["label_distribution"] = str(_plot_label_distribution(output_dir / "label_distribution.png", rows))
    if history:
        plots["training_curves"] = str(_plot_training_curves(output_dir / "training_curves.png", history))
    if feature_importances:
        plots["feature_importance"] = str(
            _plot_feature_importance(output_dir / "feature_importance.png", feature_importances)
        )
    return plots


def _configure_matplotlib():
    import matplotlib

    matplotlib.use("Agg")


def _plot_label_distribution(path: Path, rows: list[TrainingRow]) -> Path:
    import matplotlib.pyplot as plt

    labels = np.array([row.label for row in rows], dtype=np.float32)
    fig, ax = plt.subplots(figsize=(10, 6))
    ax.hist(labels, bins=40, color="#2563eb", edgecolor="white", alpha=0.9)
    ax.set_title("Training Label Distribution")
    ax.set_xlabel("Label")
    ax.set_ylabel("Frequency")
    ax.grid(alpha=0.2)
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)
    return path


def _plot_training_curves(path: Path, history: list[TrainingHistoryPoint]) -> Path:
    import matplotlib.pyplot as plt

    iterations = [point.iteration for point in history]
    train_rmse = [point.train_rmse for point in history]
    validation_rmse = [point.validation_rmse for point in history]
    train_mae = [point.train_mae for point in history]
    validation_mae = [point.validation_mae for point in history]

    fig, axes = plt.subplots(1, 2, figsize=(14, 5))
    axes[0].plot(iterations, train_rmse, label="train_rmse", color="#2563eb")
    axes[0].plot(iterations, validation_rmse, label="validation_rmse", color="#dc2626")
    axes[0].set_title("RMSE by Iteration")
    axes[0].set_xlabel("Iteration")
    axes[0].set_ylabel("RMSE")
    axes[0].grid(alpha=0.2)
    axes[0].legend()

    axes[1].plot(iterations, train_mae, label="train_mae", color="#16a34a")
    axes[1].plot(iterations, validation_mae, label="validation_mae", color="#f59e0b")
    axes[1].set_title("MAE by Iteration")
    axes[1].set_xlabel("Iteration")
    axes[1].set_ylabel("MAE")
    axes[1].grid(alpha=0.2)
    axes[1].legend()

    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)
    return path


def _plot_feature_importance(path: Path, feature_importances: dict[str, float]) -> Path:
    import matplotlib.pyplot as plt

    ordered = sorted(feature_importances.items(), key=lambda item: item[1], reverse=True)[:15]
    labels = [item[0] for item in ordered]
    values = [item[1] for item in ordered]

    fig, ax = plt.subplots(figsize=(10, 7))
    ax.barh(labels[::-1], values[::-1], color="#7c3aed")
    ax.set_title("Top Feature Importances")
    ax.set_xlabel("Importance")
    ax.grid(axis="x", alpha=0.2)
    fig.tight_layout()
    fig.savefig(path, dpi=150)
    plt.close(fig)
    return path
