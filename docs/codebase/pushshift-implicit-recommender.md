# Pushshift-only implicit-feedback recommender

This project uses the Pushshift Reddit dataset (April 2019) as training data for a content-popularity ranking model. The system does not include exposure-level behavior logs such as impressions, clicks, dwell time, or feed ranking position. Instead, Reddit engagement metrics (score, num_comments, num_crossposts, upvote_ratio) are used as implicit popularity signals.

## Current implementation

The training pipeline uses a **regression** approach on `log1p(popularity)` where `popularity = score + num_comments + num_crossposts`. This is a content-popularity proxy, not a personalized CTR model.

### Training signals extracted from Pushshift

- `score` (net upvotes) → used as `upvote_count` feature and in popularity label
- `upvote_ratio` → extracted directly from Reddit data (0.0–1.0)
- `num_comments` → used as `comment_count` feature and in popularity label
- `num_crossposts` → used as `share_count` feature and in popularity label
- `author_created_utc` → used to compute author seniority
- `created_utc` → used to compute post age and Reddit-style hot score
- multimedia detection (is_video, media, thumbnail, url extension)
- crosspost detection (num_crossposts > 0 or crosspost_parent present)

### What the model learns

The model learns to predict content popularity based on:
- Content characteristics (length, multimedia, share status)
- Temporal signals (post age, Reddit-style hot score)
- Engagement ratio (upvote_ratio from Reddit)
- Author reputation (seniority, post count, average popularity)

## In scope

- content-popularity ranking from Reddit engagement metrics
- author-level feature aggregation
- Reddit-style hot score computation (aligned between training and inference)
- real upvote_ratio extraction from Pushshift data
- chronological-hash-based train/validation split (deterministic, reproducible)

## Out of scope

- impression logging for training
- feature snapshots captured at impression time
- CTR prediction
- exposure-aware ranking
- dwell-time training labels
- true negative labels from shown-but-skipped items
- user behavior tracking (interaction_count, affinity_score)

## Feature alignment guarantees

Both training and inference use the same:
- Feature names and order (`RankingFeatureSchema.FEATURE_ORDER`)
- Hot score formula (Reddit-style: sign * log10(|score|) + time_component)
- Upvote ratio semantics (real ratio, not hardcoded)
- Default values for unimplemented behavior slots (0.0 for interactions, 999.0 for hours_since_last_interaction)

## Leakage prevention

- split train/validation by deterministic hash of post_id (MD5 mod 5 → ~80/20)
- author aggregates computed from all scanned posts (not just sampled)
- reservoir sampling ensures unbiased selection from stream
- no future data leakage since all features are computed relative to `reference_utc`

## Future improvements (when behavior tracking is added)

When user behavior tracking is implemented, fill these feature slots:
- `interaction_count_7d` (slot 9)
- `interaction_count_30d` (slot 10)
- `hours_since_last_interaction` (slot 11)
- `affinity_score` (slot 12)

Then retrain with the same pipeline and bump `feature_schema_version` to `v2`.
