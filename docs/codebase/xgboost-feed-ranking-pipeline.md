# XGBoost Feed Ranking Pipeline

## Goal

Train a XGBoost-compatible ranking model offline from the Pushshift Reddit dataset, export one deployable model artifact, and reuse that artifact inside the Java backend to score feed candidates.

## Recommended split

1. Offline training pipeline
   - stream submissions from Pushshift `.zst` archives
   - extract real Reddit signals: `upvote_ratio`, `score`, `num_comments`, `num_crossposts`
   - compute Reddit-style hot score: `sign(score) * log10(|score|) + seconds/45000`
   - aggregate author-level features (post count, average popularity, seniority)
   - reservoir-sample posts, build feature rows aligned with `RankingFeatureSchema.FEATURE_ORDER`
   - train gradient-boosted regression tree on `log1p(popularity)` label
   - export one JSON artifact containing metadata plus `model_dump`

2. Online inference in backend
   - `FeatureExtractionService` builds `RankingFeatures` from live app data
   - computes Reddit-aligned hot score: `sign(netScore) * log10(max(|netScore|, 1)) + postAgeHours / 12.5`
   - computes real `upvote_ratio` from upvote/downvote counts
   - `FeatureVectorizer` converts them to the training feature contract
   - `RankingService` loads the exported artifact and scores each post
   - `FeedRankingService` sorts posts by model score and falls back safely if the model is unavailable

## Feature schema (v1)

19 features in fixed order, defined in `RankingFeatureSchema.FEATURE_ORDER`:

| # | Feature | Training source | Inference source |
|---|---------|----------------|-----------------|
| 0 | content_length | title + body length | post content length |
| 1 | has_multimedia | video/media/thumbnail/url detection | imageUrl present |
| 2 | is_share_post | num_crossposts > 0 | PostType.SHARE |
| 3 | post_age_hours | (reference_utc - created_utc) / 3600 | ChronoUnit minutes / 60 |
| 4 | hot_score | Reddit formula: sign * log10(\|score\|) + seconds/45000 | sign * log10(max(\|netScore\|, 1)) + hours/12.5 |
| 5 | upvote_ratio | Reddit `upvote_ratio` field (0.0–1.0) | upvotes / (upvotes + downvotes) |
| 6 | author_seniority | (created_utc - author_created_utc) / seconds_per_year | days since user registration / 365 |
| 7 | author_post_count | aggregate from scanned posts | DB count |
| 8 | author_engagement_rate | average popularity across author's posts | DB average popularity |
| 9 | interaction_count_7d | 0.0 when Reddit behavior data is unavailable | viewer-author interactions in the last 7 days |
| 10 | interaction_count_30d | 0.0 when Reddit behavior data is unavailable | viewer-author interactions in the last 30 days |
| 11 | hours_since_last_interaction | 999.0 cold-start default | hours since the viewer last interacted with the author, or 999.0 when none exists |
| 12 | affinity_score | 0.0 when Reddit behavior data is unavailable | 30-day viewer-author interactions divided by viewer total interactions |
| 13 | upvote_count | Reddit score (net upvotes) | post upvote count |
| 14 | downvote_count | 0.0 (Reddit doesn't expose) | post downvote count |
| 15 | comment_count | num_comments | post comment count |
| 16 | share_count | num_crossposts | post share count |
| 17 | view_count | 0.0 (Reddit doesn't expose) | post view count |
| 18 | popularity | score + num_comments + num_crossposts | upvotes + comments + shares |

## Training CLI usage

```bash
./mvnw compile exec:java \
  -Dexec.mainClass="com.socialpulse.app.ai.training.PushshiftTrainingCli" \
  -Dexec.arguments="--submissions,path/to/RS_2019-04.zst,--comments,path/to/RC_2019-04.zst,--output,src/main/resources/ai/ranking-model.json,--sample-size,12000,--scan-limit-posts,30000,--scan-limit-comments,100000,--n-estimators,16,--max-depth,3,--min-samples-leaf,64,--max-thresholds,16,--learning-rate,0.18,--seed,42"
```

## Artifact shape

```json
{
  "artifact_version": "1",
  "feature_schema_version": "v1",
  "training_dataset": "pushshift_reddit_apr2019",
  "trained_at": "2026-05-16T12:19:48Z",
  "label_strategy": "log_popularity_proxy",
  "training_summary": { "metrics": { "train_rmse": 0.118, "validation_rmse": 0.109 } },
  "model_dump": { "objective": "regression", "feature_names": [...], "tree_info": [...] }
}
```

## Important rules

- Training code must emit the exact same feature names and preprocessing defaults as `FeatureVectorizer`.
- If you change features, bump `feature_schema_version` and deploy the new artifact together with backend config.
- `hot_score` must use the same formula in both training and inference. Currently both use the Reddit-style time-decayed formula.
- `upvote_ratio` must be extracted from real data (not hardcoded) in training, and computed from real counts at inference.
