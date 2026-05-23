# Current AI Structure And Interactions

This document describes the current AI integration after the schema v2 cleanup.

## Components

| Component | Path | Role |
|---|---|---|
| Training scanner | `ai_pipeline/training/scanner.py` | Reads Pushshift `.zst` files and filters rows |
| Feature engineering | `ai_pipeline/training/feature_engineering.py` | Builds leakage-safe v2 rows |
| Trainer | `ai_pipeline/training/trainer.py` | Trains LightGBM and computes metrics |
| Inference service | `ai_pipeline/inference/ranking_service.py` | Loads `model.json` + `model.txt` |
| Vectorizer | `ai_pipeline/inference/vectorizer.py` | Applies schema order and preprocessing |
| Backend extractor | `backend/.../PostFeatureExtractor.java` | Builds live post features |
| Backend ranking | `backend/.../FeedRankingService.java` | Calls AI and falls back safely |

## Model Runtime

The Python service owns model loading and prediction. Java backend no longer
contains a local tree scorer. This keeps runtime behavior aligned with the
actual LightGBM booster generated during training.

## Feature Contract

Schema version: `v2`

The schema contains only features available before or at serving time:

- post structure and age
- historical author information
- viewer-author prior interactions

Final engagement snapshots from the training archive are not sent as features.

## Request Flow

1. `FeedRankingService` selects candidates.
2. `FeatureExtractionService` builds `RankingFeatures`.
3. `AiRankingClient` sends the request to the AI service.
4. `RankingService` validates artifact/schema and predicts with LightGBM.
5. Backend accepts predictions only if IDs and schema match.
6. Backend sorts the feed and caches the result.

## Failure Mode

When AI is disabled, unavailable, returns invalid scores, or has a schema
mismatch, backend uses `FallbackRankingService`. This fallback may use live DB
counters but it is separate from the AI model feature schema.
