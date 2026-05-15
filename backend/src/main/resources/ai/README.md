Place the exported LightGBM ranking model at:

`classpath:ai/lightgbm-ranking-model.json`

This Java scorer expects the JSON structure produced by LightGBM's Python `Booster.dump_model()`, not the plain-text `model.txt` dump.

Minimal export example after training in Python:

```python
import json

with open("lightgbm-ranking-model.json", "w", encoding="utf-8") as f:
    json.dump(booster.dump_model(), f)
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

Scoring behavior:

- If a model feature name is absent from the produced feature map, the scorer uses `0.0`.
- If a feature value is explicitly `null` or `NaN`, the scorer follows the tree node's `default_left`.
- Supported decision types: `<=`, `<`, `>=`, `>`, `==`
- Unsupported categorical LightGBM split types are not handled by this scorer.

Fallback behavior:

- `ai.lightgbm.enabled=true` and model load success: use local LightGBM scorer
- Otherwise: fall back to the existing HTTP AI ranking service
