# 02 - Data Ingestion

The training pipeline reads two Pushshift Reddit files:

| File | Meaning |
|---|---|
| `RS_2019-04.zst` | Submissions/posts |
| `RC_2019-04.zst` | Comments/interactions |

The files should stay under `ai_pipeline/data/` and are ignored by Git.

## Submission Scan

`PushshiftDatasetScanner.scan_submissions()` streams the submissions file and
keeps only rows suitable for training.

Accepted rows must have:

- valid `id`, `author`, and `created_utc`
- enough textual content
- non-deleted body/title
- non-bot author
- non-NSFW content when `exclude_nsfw` is enabled
- enough alpha characters and distinct tokens
- acceptable URL count

The scanner also deduplicates posts by normalized text signature when
`dedupe_posts` is enabled.

## Label Source

The final Reddit engagement snapshot is used only to build the target label:

```text
popularity = max(score, 0) + max(num_comments, 0) + max(num_crossposts, 0)
label = log1p(popularity)
```

These final snapshot values are not exported as model features.

## Author Aggregates

Author aggregates are updated chronologically while submissions are scanned.
For each current post, the feature row receives the author's historical state
before that post is added to the aggregate.

This keeps `author_post_count` and `author_engagement_rate` from looking into
the current post's final result.

## Comment Scan

`scan_interactions()` reads comments and extracts viewer-author interactions for
sampled post authors. Only timestamps before the candidate post are used when
features are built.

## Progress Output

Both scanners print progress lines containing:

- file percentage
- scanned row count
- accepted row count
- scan rate
- filter statistics

This makes long runs observable without opening generated artifacts.
