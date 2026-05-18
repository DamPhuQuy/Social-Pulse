# 04 — Preprocessing

## Tổng quan

Bước Preprocessing biến đổi feature matrix sau khi xây dựng, trước khi đưa vào model training. Mục đích: xử lý outliers, chuẩn hóa phân phối skewed, và validate data quality.

**File chính:** `training/feature_engineering.py` (method `_preprocess_features`)

## Pipeline Preprocessing

```
Raw Features → Outlier Capping → Log Transform → Validation → Training
```

## Bước 1: Outlier Capping (Winsorization)

### Vấn đề
Social media data có phân phối **heavy-tailed**: đa số posts có score thấp, nhưng viral posts có score cực lớn (100k+). Outliers này gây bias cho tree splits.

### Giải pháp
Cap giá trị tại **percentile 99** cho các features dễ bị outlier:

```python
_OUTLIER_PERCENTILE = 99.0

_CAP_FEATURES = {
    "content_length",
    "post_age_hours",
    "hot_score",
    "author_seniority",
    "author_post_count",
    "author_engagement_rate",
    "hours_since_last_interaction",
}

for i, name in enumerate(feature_names):
    if name in _CAP_FEATURES:
        cap = float(np.percentile(matrix[:, i], _OUTLIER_PERCENTILE))
        if cap > 0:
            matrix[:, i] = np.minimum(matrix[:, i], cap)
```

### Ví dụ
| Feature | Trước cap | Sau cap (P99=5000) |
|---------|-----------|-------------------|
| `content_length` | [50, 200, 500, **50000**] | [50, 200, 500, **5000**] |

### Tại sao không cap count features?
Count features (`upvote_count`, `comment_count`, ...) sẽ được log-transform ở bước sau — log tự nhiên nén outliers.

## Bước 2: Log Transform

### Vấn đề
Count-based features có phân phối **power-law** (lệch phải cực mạnh):
- 90% posts có < 10 comments
- 1% posts có > 1000 comments

Tree-based models tìm split tốt hơn khi phân phối đều hơn.

### Giải pháp
Áp dụng `log1p(x)` cho các skewed count features:

```python
_LOG_TRANSFORM_FEATURES = {
    "upvote_count",
    "downvote_count",
    "comment_count",
    "share_count",
    "view_count",
    "popularity",
    "interaction_count_7d",
    "interaction_count_30d",
}

for i, name in enumerate(feature_names):
    if name in _LOG_TRANSFORM_FEATURES:
        matrix[:, i] = np.log1p(np.maximum(matrix[:, i], 0.0))
```

### Tại sao `log1p` thay vì `log`?
- `log(0)` = -∞ → crash
- `log1p(0)` = 0 → an toàn cho zero-values (rất phổ biến trong count data)

### Ví dụ transformation
| Raw value | log1p(value) |
|-----------|-------------|
| 0 | 0.000 |
| 1 | 0.693 |
| 10 | 2.398 |
| 100 | 4.615 |
| 1000 | 6.909 |
| 100000 | 11.513 |

Khoảng cách giữa 0 và 100 (4.615) gần bằng khoảng cách giữa 100 và 100000 (6.898) — giúp tree splits hiệu quả hơn.

## Bước 3: Data Validation

Sau preprocessing, validate toàn bộ matrix:

```python
def _validate_rows(rows: list[TrainingRow]) -> None:
    for i, row in enumerate(rows):
        for j, val in enumerate(row.features):
            if math.isnan(val) or math.isinf(val):
                raise ValueError(f"Invalid value in row {i}, feature '{feature_name}': {val}")
```

**Kiểm tra:**
- Không có `NaN` (Not a Number)
- Không có `Inf` (Infinity)
- Nếu phát hiện → raise error ngay, không train model lỗi

## Bước 4: Distribution Statistics

Tính toán thống kê phân phối cho monitoring:

```python
stats = {
    "total_training_rows": len(rows),
    "label_stats": {
        "mean": float(labels.mean()),
        "std": float(labels.std()),
        "min": float(labels.min()),
        "max": float(labels.max()),
        "zero_ratio": float((labels == 0).sum() / len(labels)),
    },
    "feature_stats": {
        "content_length": {"mean": ..., "std": ..., "min": ..., "max": ..., "zero_ratio": ...},
        ...
    }
}
```

**Mục đích:**
- Phát hiện **data drift** giữa các lần train
- Kiểm tra label balance (zero_ratio quá cao = model bias)
- Verify preprocessing hoạt động đúng (max values hợp lý)

## Train-Serve Consistency

**Critical:** Inference phải apply **cùng transforms** như training.

### Training (feature_engineering.py)
```python
# Log-transform count features
matrix[:, i] = np.log1p(np.maximum(matrix[:, i], 0.0))
```

### Inference (vectorizer.py)
```python
# Same log-transform applied at serving time
_LOG_TRANSFORM_FEATURES = {
    "upvote_count", "downvote_count", "comment_count",
    "share_count", "view_count", "popularity",
    "interaction_count_7d", "interaction_count_30d",
}

for key in _LOG_TRANSFORM_FEATURES:
    v[key] = math.log1p(max(v[key], 0.0))
```

**Nếu không nhất quán → Train-Serve Skew:**
- Model train trên `log1p(1000) = 6.9`
- Inference gửi raw `1000`
- Model nhận giá trị ngoài phân phối đã học → prediction sai

## Features KHÔNG được transform

| Feature | Lý do không transform |
|---------|----------------------|
| `has_multimedia` | Binary (0/1) — đã chuẩn |
| `is_share_post` | Binary (0/1) — đã chuẩn |
| `upvote_ratio` | Bounded [0, 1] — đã chuẩn |
| `affinity_score` | Bounded [0, 1] — đã chuẩn |

## Tại sao không Feature Scaling (StandardScaler)?

Tree-based models (GBDT) **không cần feature scaling** vì:
- Splits dựa trên threshold comparison (`x <= t`)
- Không dùng distance metrics
- Không bị ảnh hưởng bởi magnitude khác nhau giữa features

→ Chỉ cần xử lý outliers và skewness, không cần normalize về [0,1] hay z-score.
