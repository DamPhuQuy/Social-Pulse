# LightGBM Feed Ranking Pipeline

## Goal

Train a LightGBM ranking model offline from the Pushshift Reddit dataset, export one deployable model artifact, and reuse that artifact inside the Java backend to score feed candidates.

## Recommended split

1. Offline training pipeline
   - build implicit positives from submissions, comments, and replies
   - sample weak negatives chronologically
   - generate feature rows with the same names used by backend inference
   - train LightGBM ranker or binary rank proxy
   - export one JSON artifact containing metadata plus `model_dump`

2. Online inference in backend
   - `FeatureExtractionService` builds `RankingFeatures` from live app data
   - `LightGbmFeatureVectorizer` converts them to the training feature contract
   - `LightGbmRankingService` loads the exported artifact and scores each post
   - `FeedRankingService` sorts posts by model score and falls back safely if the model is unavailable

## Current backend contract

- Feature schema version is centralized in `com.socialpulse.app.ai.lightgbm.LightGbmFeatureSchema`
- Feed ranking request schema now comes from config instead of hardcoded `v1`
- Local model loader supports:
  - raw `booster.dump_model()`
  - wrapped pipeline artifact with `model_dump`

## Artifact shape

```json
{
  "artifact_version": "1",
  "feature_schema_version": "v1",
  "training_dataset": "pushshift_reddit",
  "trained_at": "2026-05-15T00:00:00Z",
  "label_strategy": "implicit_pairwise",
  "model_dump": {}
}
```

## Important rule

Training code must emit the exact same feature names and preprocessing defaults as `LightGbmFeatureVectorizer`. If you change features, bump `feature_schema_version` and deploy the new artifact together with backend config.
