Place the exported LightGBM ranking model at:

`classpath:ai/lightgbm-ranking-model.json`

This Java scorer accepts either:

- the raw JSON produced by LightGBM's Python `Booster.dump_model()`
- or a wrapped artifact exported by your training pipeline with metadata plus `model_dump`

It does not accept the plain-text `model.txt` dump.

Minimal raw export example after training in Python:

```python
import json

with open("lightgbm-ranking-model.json", "w", encoding="utf-8") as f:
    json.dump(booster.dump_model(), f)
```

Recommended pipeline artifact export:

```python
import json
from datetime import datetime, timezone

artifact = {
    "artifact_version": "1",
    "feature_schema_version": "v1",
    "training_dataset": "pushshift_reddit",
    "trained_at": datetime.now(timezone.utc).isoformat(),
    "label_strategy": "implicit_pairwise",
    "model_dump": booster.dump_model(),
}

with open("lightgbm-ranking-model.json", "w", encoding="utf-8") as f:
    json.dump(artifact, f)
```

Feature mapping contract:

- The Java scorer matches features by `feature_names` from the dump, not only by index.
- Training must use the same feature names and the same preprocessing defaults.
- The backend vectorizer still emits a stable order, which should also be the training order for reproducibility:

1. `content_length`
2. `has_multimedia`
3. `is_share_post`
4. `post_age_hours`
5. `hot_score`
6. `upvote_ratio`
7. `author_seniority`
8. `author_post_count`
9. `author_engagement_rate`
10. `interaction_count_7d`
11. `interaction_count_30d`
12. `hours_since_last_interaction`
13. `affinity_score`
14. `upvote_count`
15. `downvote_count`
16. `comment_count`
17. `share_count`
18. `view_count`
19. `popularity`

Default preprocessing values used by the backend vectorizer:

- Most missing numeric features: `0.0`
- `upvote_ratio`: `0.5`
- `hours_since_last_interaction`: `999.0`
- Missing boolean flags: `0.0`
- Default schema version: `v1`

Scoring behavior:

- If a model feature name is absent from the produced feature map, the scorer uses `0.0`.
- If a feature value is explicitly `null` or `NaN`, the scorer follows the tree node's `default_left`.
- Supported decision types: `<=`, `<`, `>=`, `>`, `==`
- Unsupported categorical LightGBM split types are not handled by this scorer.

Fallback behavior:

- `ai.lightgbm.enabled=true` and model load success: use local LightGBM scorer
- Otherwise: return no model predictions and let `FeedRankingService` use deterministic fallback ranking
- If the wrapped artifact's `feature_schema_version` differs from `ai.lightgbm.feature-schema-version`, the model is rejected
