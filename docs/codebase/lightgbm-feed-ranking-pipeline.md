# LightGBM Feed Ranking Pipeline

## Goal

Train a LightGBM ranking/regression model offline from Pushshift Reddit data and
serve it through the Python AI service. The Java backend does not parse tree
artifacts directly; it sends v2 feature payloads to the AI service.

## Feature Schema v2

| # | Feature | Training source | Serving source |
|---|---|---|---|
| 0 | `content_length` | title + body length | post content length |
| 1 | `has_multimedia` | media/url/thumbnail detection | image URL present |
| 2 | `is_share_post` | crosspost/share indicator | shared post flag |
| 3 | `post_age_hours` | reference time minus created time | current time minus created time |
| 4 | `author_seniority` | historical author account age | user registration age |
| 5 | `author_post_count` | historical author count | DB count |
| 6 | `author_engagement_rate` | historical author average popularity | DB average popularity |
| 7 | `interaction_count_7d` | prior viewer-author comments | live viewer-author interactions |
| 8 | `interaction_count_30d` | prior viewer-author comments | live viewer-author interactions |
| 9 | `hours_since_last_interaction` | latest prior interaction recency | live interaction recency |
| 10 | `affinity_score` | viewer-author interaction share | live interaction share |

Target-time engagement snapshots from Reddit are excluded from features to
avoid label leakage.

## Artifacts

| File | Purpose |
|---|---|
| `model.json` | schema, preprocessing, metrics, diagnostics |
| `model.txt` | LightGBM booster |
| `metrics.json` | training report |
| `plots/*.png` | visual checks |

## Important Rules

- Backend and AI service must use the same `feature_schema_version`.
- A trained artifact is valid only with the matching `model.txt` sidecar.
- Split integrity must show zero post overlap before accepting final metrics.
- If model prediction fails, backend uses deterministic fallback ranking.
