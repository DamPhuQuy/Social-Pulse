# 06 - Evaluation

The final metrics are stored in `ai_pipeline/model/metrics.json`.

## Core Metrics

| Metric | Meaning |
|---|---|
| RMSE | Average squared prediction error, sensitive to large misses |
| MAE | Average absolute prediction error |
| R2 | Improvement over mean-label baseline |
| NDCG@10 | Ranking quality inside post groups |

Compare validation and test together. A good run should not have a large gap
between them.

## Baseline

`evaluation_diagnostics.mean_label_baseline` records the result of predicting
the training label mean for every row. The model should beat this baseline on
validation and test.

## Leakage Diagnostics

Check:

```json
{
  "evaluation_diagnostics": {
    "split_integrity": {
      "post_id_overlap": {
        "train_validation": 0,
        "train_test": 0,
        "validation_test": 0
      }
    }
  }
}
```

Any non-zero overlap means the run is invalid.

## Plots

| Plot | Purpose |
|---|---|
| `label_distribution.png` | Overall target distribution |
| `split_label_distribution.png` | Train/validation/test target shift |
| `training_curves.png` | Overfit check by iteration |
| `feature_importance.png` | Gain-based feature importance |

Use plots together with metrics; feature importance alone is not enough to prove
model quality.
