# 04 - Preprocessing

Preprocessing is learned from the training split and then applied unchanged to
validation, test, and inference.

## Transform Steps

1. Replace missing/invalid numeric values with schema defaults.
2. Cap selected long-tail features at the training-set p99.
3. Apply `log1p` to selected count features.
4. Preserve binary features as `0.0` or `1.0`.

## Capped Features

- `content_length`
- `post_age_hours`
- `author_seniority`
- `author_post_count`
- `author_engagement_rate`
- `hours_since_last_interaction`

## Log Features

- `interaction_count_7d`
- `interaction_count_30d`

## Defaults

| Feature group | Default |
|---|---|
| Generic numeric | `0.0` |
| Missing last interaction | `999.0` |
| Binary flags | `0.0` |

## Artifact

The learned preprocessing values are stored inside `model.json`:

```json
{
  "preprocessing": {
    "caps": {},
    "log_transform_features": [],
    "defaults": {}
  }
}
```

Inference must use this artifact instead of recomputing preprocessing from live
traffic.
