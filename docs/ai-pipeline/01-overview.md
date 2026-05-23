# 01 - AI Pipeline Overview

Social Pulse uses an offline LightGBM ranking model to score feed candidates.
The backend extracts the same leakage-safe feature contract used during
training, sends it to the FastAPI AI service, and falls back to deterministic
ranking when the AI service is unavailable.

## Current Contract

| Item | Value |
|---|---|
| Model backend | LightGBM |
| Feature schema | `v2` |
| Metadata artifact | `ai_pipeline/model/model.json` |
| Booster artifact | `ai_pipeline/model/model.txt` |
| Label | `log1p(score + num_comments + num_crossposts)` |
| Split | Time ordered and grouped by `post_id` |

Schema v2 intentionally excludes Reddit target-time engagement snapshots from
features. Those values are allowed only as labels or historical author
aggregates.

## Runtime Flow

1. Backend selects candidate posts.
2. Backend extracts post, author, and viewer-author interaction features.
3. Backend sends a `feature_schema_version = "v2"` request to the AI service.
4. AI service vectorizes features in `RankingFeatureSchema.FEATURE_ORDER`.
5. LightGBM predicts scores.
6. Backend sorts feed items by predicted score and applies fallback when needed.

## Training Flow

1. Stream Pushshift submissions and comments from `.zst` files.
2. Filter deleted, low-quality, duplicated, bot-like, and noisy content.
3. Build leakage-safe feature rows with historical author/interactions only.
4. Split rows by unique `post_id` in chronological order.
5. Train LightGBM with validation early stopping.
6. Write metrics, diagnostics, and plots.

## Files

| Path | Purpose |
|---|---|
| `ai_pipeline/training/scanner.py` | Streams Pushshift files and filters raw data |
| `ai_pipeline/training/feature_engineering.py` | Builds v2 training rows |
| `ai_pipeline/training/trainer.py` | Trains/evaluates LightGBM |
| `ai_pipeline/inference/vectorizer.py` | Converts request DTOs to feature vectors |
| `ai_pipeline/inference/ranking_service.py` | Loads `model.json` + `model.txt` and predicts |
| `backend/.../PostFeatureExtractor.java` | Builds serving-time post features |

## Final Train Checklist

- `evaluation_warnings` is empty or understood.
- `split_integrity.post_id_overlap.*` is `0`.
- Validation and test metrics are close.
- NDCG is not suspiciously perfect.
- Top feature gain share is not extreme.
- Plots exist under `ai_pipeline/model/plots`.
