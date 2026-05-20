# 07 — Inference

## Tổng quan

Inference pipeline nhận features từ Spring Boot backend, chuyển đổi thành feature vector, và scoring bằng model đã train. Kết quả là relevance score cho mỗi post, dùng để xếp hạng feed.

**Files chính:**
- `inference/vectorizer.py` — Feature vector construction
- `inference/ranking_service.py` — Model loading + scoring
- `shared/scorer.py` — Tree traversal engine
- `server.py` — FastAPI HTTP server

## Luồng xử lý

```
Backend Request → FastAPI → Vectorizer → Scorer → Response
     │                          │            │
     │                          │            ▼
     │                          │      model.json (loaded once)
     │                          ▼
     │                   Apply log-transform
     │                   (train-serve consistency)
     ▼
  RankingFeatures {
    post_id, post_features,
    author_features, interaction_features
  }
```

## 1. Feature Vectorizer

### Input: Structured features từ backend

```python
@dataclass
class RankingFeatures:
    post_id: int
    post_features: PostFeatures | None
    author_features: AuthorFeatures | None
    interaction_features: InteractionFeatures | None
```

### Processing

```python
def to_feature_map(self, features: RankingFeatures) -> dict[str, float]:
    # 1. Extract raw values (với defaults cho missing)
    v["content_length"] = safe_int(pf.content_length)
    v["upvote_ratio"] = safe(pf.upvote_ratio, default=0.5)
    v["hours_since_last_interaction"] = safe(inf.hours_since_last_interaction, default=999.0)
    ...

    # 2. Compute derived features
    v["popularity"] = safe(pf.popularity, default=up + cmt + share)

    # 3. Apply log-transform (MUST match training preprocessing)
    for key in _LOG_TRANSFORM_FEATURES:
        v[key] = math.log1p(max(v[key], 0.0))

    return v
```

### Default values (missing feature handling)

| Feature | Default | Lý do |
|---------|---------|-------|
| Numeric | `0.0` | Neutral |
| `upvote_ratio` | `0.5` | Trung bình |
| `hours_since_last_interaction` | `999.0` | Cold start (chưa tương tác) |
| Binary features | `0.0` (False) | Conservative |

### Log-transform (train-serve consistency)

```python
_LOG_TRANSFORM_FEATURES = {
    "upvote_count", "downvote_count", "comment_count",
    "share_count", "view_count", "popularity",
    "interaction_count_7d", "interaction_count_30d",
}
```

**Quan trọng:** Set này PHẢI giống hệt `_LOG_TRANSFORM_FEATURES` trong `feature_engineering.py`.

## 2. Model Loading (Lazy + Thread-safe)

```python
class RankingService:
    def _get_or_load_scorer(self) -> TreeModelScorer | None:
        if self._scorer is not None:
            return self._scorer
        with self._lock:  # Double-checked locking
            if self._scorer is not None:
                return self._scorer
            self._scorer = self._load_scorer()
            return self._scorer
```

**Đặc điểm:**
- **Lazy loading:** Model chỉ load khi có request đầu tiên
- **Thread-safe:** `threading.Lock` đảm bảo chỉ load 1 lần
- **Double-checked locking:** Tránh lock contention sau khi đã load

### Model artifact format

```json
{
  "artifact_version": "1",
  "feature_schema_version": "v1",
  "training_dataset": "pushshift_reddit_apr2019",
  "trained_at": "2024-01-15T10:30:00Z",
  "model_dump": {
    "objective": "regression",
    "feature_names": ["content_length", ...],
    "tree_info": [...]
  }
}
```

### Schema version validation

```python
if schema_ver != self._properties.feature_schema_version:
    logger.warning("Artifact schema mismatch")
    return None  # Refuse to score with incompatible model
```

Nếu model được train với schema khác → từ chối scoring, trả empty response.

## 3. Tree Traversal Scorer

```python
class TreeModelScorer:
    def score(self, features: dict[str, float]) -> float:
        total = 0.0
        for tree_info in self._model.tree_info:
            tree_score = self._score_node(tree_info.tree_structure, features)
            total += tree_score * tree_info.shrinkage
        return total
```

### Node traversal logic

```python
def _score_node(self, node, features):
    if node.is_leaf:
        return node.leaf_value

    feature_name = self._model.get_feature_name(node.split_feature)
    value, missing = self._resolve_feature(feature_name, features)

    if missing:
        go_left = node.default_left  # Missing value direction
    else:
        go_left = (value <= node.threshold)  # Standard comparison

    return self._score_node(left_child if go_left else right_child, features)
```

### Decision types supported

| Type | Condition |
|------|-----------|
| `<=` (default) | `value <= threshold` |
| `<` | `value < threshold` |
| `>` | `value > threshold` |
| `>=` | `value >= threshold` |
| `==` | `value == threshold` |

## 4. API Server

### Endpoint: POST /api/ranking/predict

**Request:**
```json
{
  "feature_schema_version": "v1",
  "features": [
    {
      "post_id": 123,
      "post_features": {
        "content_length": 500,
        "has_multimedia": true,
        "upvote_count": 42,
        "comment_count": 15,
        "post_age_hours": 2.5
      },
      "author_features": {
        "seniority_years": 3.2,
        "post_count": 150
      },
      "interaction_features": {
        "interaction_count_7d": 5,
        "affinity_score": 0.3
      }
    }
  ]
}
```

**Response:**
```json
[
  {
    "post_id": 123,
    "score": 3.456,
    "feature_schema_version": "v1"
  }
]
```

### Endpoint: GET /health

```json
{"status": "ok"}
```

## Error Handling

| Scenario | Behavior |
|----------|----------|
| Model file not found | Log warning, return empty `[]` |
| Schema version mismatch | Log warning, return empty `[]` |
| AI disabled (`AI_PIPELINE_ENABLED=false`) | Return empty `[]` |
| Invalid model JSON | Log warning, return empty `[]` |
| Missing features in request | Use defaults (graceful degradation) |

**Design principle:** Never crash — always return empty results on error. Backend sẽ fallback sang deterministic ranking.

## Configuration (Environment Variables)

| Variable | Default | Mô tả |
|----------|---------|--------|
| `AI_PIPELINE_ENABLED` | `true` | Enable/disable scoring |
| `AI_PIPELINE_MODEL_LOCATION` | `ai_pipeline/model/model.json` | Path tới model artifact |
| `AI_PIPELINE_FEATURE_SCHEMA_VERSION` | `v1` | Expected schema version |
| `AI_PIPELINE_INFERENCE_DEVICE` | `cpu` | Device cho XGBoost inference (`cpu` hoặc `cuda`) |

Legacy env vars `AI_ENABLED`, `AI_MODEL_LOCATION`, `AI_FEATURE_SCHEMA_VERSION` vẫn được đọc để tương thích ngược.

## Performance

- **Model load:** ~100ms (one-time, lazy)
- **Per-request scoring:** O(n_trees × tree_depth × n_posts)
- **Typical latency:** < 10ms cho 50 posts (200 trees, depth 3)
- **Memory:** Model JSON in-memory (~1-5 MB)
