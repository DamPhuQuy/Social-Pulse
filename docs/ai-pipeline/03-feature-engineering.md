# 03 - Feature Engineering

Feature schema v2 has 11 features. The fixed order is defined in
`ai_pipeline/shared/schema.py` and must match backend extraction and inference
vectorization.

## Feature Schema v2

| # | Feature | Source | Meaning |
|---|---|---|---|
| 0 | `content_length` | Post | Title/body length in training, post content length in serving |
| 1 | `has_multimedia` | Post | Image/video/media indicator |
| 2 | `is_share_post` | Post | Shared/crosspost indicator |
| 3 | `post_age_hours` | Post | Hours between reference time and post creation |
| 4 | `author_seniority` | Author | Author account age in years |
| 5 | `author_post_count` | Author | Historical post count before current post |
| 6 | `author_engagement_rate` | Author | Historical average popularity before current post |
| 7 | `interaction_count_7d` | Viewer-author | Interactions in previous 7 days |
| 8 | `interaction_count_30d` | Viewer-author | Interactions in previous 30 days |
| 9 | `hours_since_last_interaction` | Viewer-author | Recency of latest prior interaction |
| 10 | `affinity_score` | Viewer-author | 30-day interaction share for the viewer |

## Leakage Guard

The model does not receive final score, final comment count, final share count,
post hotness, or vote ratio as features. Those are target-time snapshots in the
Pushshift archive and would make the model memorize the label.

## Row Types

Positive rows represent a viewer who interacted with the author's content before
the current post.

Negative rows represent sampled viewers without prior interaction with the
author. They receive zero interaction features and a zero relevance label.

Fallback rows keep posts that have no usable interaction data. They use zero
interaction features and the popularity label.

## Split Strategy

Rows are grouped by `post_id` before splitting. A single post can never appear
in more than one of train, validation, and test. Groups are ordered by creation
time to make validation/test closer to future-serving behavior.
