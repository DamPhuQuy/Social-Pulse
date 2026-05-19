# 06 — Evaluation

## Tổng quan

Bước Evaluation đánh giá chất lượng model trên cả training set và validation set, sử dụng nhiều metrics bổ sung cho nhau.

**File chính:** `training/trainer.py` (metrics computation)

## Metrics

### 1. NDCG@10 (Metric chính)

**Normalized Discounted Cumulative Gain** — metric chuẩn cho ranking.

```python
def _ndcg(rows, predictions, k=10):
    # Group rows by post_id (mỗi post có nhiều viewer rows)
    groups = defaultdict(list)
    for i, row in enumerate(rows):
        groups[row.post_id].append(i)

    for indices in groups.values():
        if len(indices) < 2:
            continue
        relevances = [rows[i].label for i in indices]
        scores = predictions[indices]

        # DCG: sum of (2^rel - 1) / log2(rank + 1)
        ranked = argsort(-scores)[:k]
        dcg = sum((2^rel[ranked[i]] - 1) / log2(i + 2) for i in range(k))

        # IDCG: DCG với perfect ranking
        ideal = argsort(-relevances)[:k]
        idcg = sum((2^rel[ideal[i]] - 1) / log2(i + 2) for i in range(k))

        ndcg = dcg / idcg
```

**Ý nghĩa:**
- NDCG = 1.0: ranking hoàn hảo
- NDCG = 0.0: ranking tệ nhất
- Penalize nhiều hơn khi relevant items bị xếp thấp (logarithmic discount)

**Grouping logic:**
- Mỗi `post_id` có nhiều rows (positive + negative viewers)
- NDCG đo: model có rank positive viewers cao hơn negative viewers không?

### 2. RMSE (Root Mean Squared Error)

```python
def _rmse(actual, predicted):
    return sqrt(mean((actual - predicted) ** 2))
```

**Ý nghĩa:** Sai số trung bình của predicted score so với actual label.

**Hạn chế cho ranking:** RMSE thấp ≠ ranking tốt. Model có thể predict sai giá trị nhưng đúng thứ tự.

### 3. MAE (Mean Absolute Error)

```python
def _mae(actual, predicted):
    return mean(abs(actual - predicted))
```

**Ý nghĩa:** Robust hơn RMSE với outliers (không bình phương sai số).

## Evaluation Strategy

### Temporal validation
- Train trên 80% data cũ nhất
- Evaluate trên 20% data mới nhất
- Mô phỏng production: predict cho posts chưa từng thấy

### Metrics được tính trên cả 2 sets

| Metric | Train | Validation | Ý nghĩa |
|--------|-------|-----------|----------|
| RMSE | ✓ | ✓ | Overfitting check: train << val = overfit |
| MAE | ✓ | ✓ | Bổ sung cho RMSE |
| NDCG@10 | ✓ | ✓ | Ranking quality |

### Overfitting detection

```
train_rmse = 0.45, val_rmse = 0.52  → OK (gap nhỏ)
train_rmse = 0.12, val_rmse = 0.89  → OVERFIT (gap lớn)
```

## Output: Metrics JSON

```json
{
  "metrics": {
    "train_rmse": 0.523456,
    "validation_rmse": 0.567890,
    "train_mae": 0.412345,
    "validation_mae": 0.445678,
    "train_ndcg_k": 0.856789,
    "validation_ndcg_k": 0.823456
  }
}
```

## Feature Importance Analysis

Sau training, model export feature importance (impurity-based):

```json
{
  "feature_importances": {
    "hot_score": 0.3245,
    "popularity": 0.2156,
    "upvote_count": 0.1432,
    "interaction_count_30d": 0.0987,
    "affinity_score": 0.0654,
    "post_age_hours": 0.0543,
    "content_length": 0.0321,
    "author_engagement_rate": 0.0234,
    ...
  }
}
```

**Cách đọc:**
- Importance cao → feature được dùng nhiều trong tree splits
- Importance = 0 → feature không hữu ích, có thể loại bỏ
- Nếu interaction features có importance thấp → cần cải thiện interaction data

## Training Summary (metrics.json)

File `model/metrics.json` chứa toàn bộ thông tin training run:

```json
{
  "scan_stats": {
    "submissions_scanned": 12000000,
    "submissions_filtered": 3500000,
    "submissions_accepted": 8500000,
    "reservoir_size": 100000
  },
  "interaction_stats": {
    "comments_scanned": 50000000,
    "interactions_extracted": 2500000,
    "unique_viewers": 450000
  },
  "feature_stats": {
    "total_training_rows": 350000,
    "label_stats": { "mean": 2.8, "std": 1.5, "zero_ratio": 0.43 },
    "feature_stats": { ... }
  },
  "train_rows": 280000,
  "validation_rows": 70000,
  "metrics": { ... },
  "hyperparameters": { ... },
  "feature_importances": { ... }
}
```

## Monitoring & Alerting

### Khi nào cần retrain?

| Signal | Threshold | Action |
|--------|-----------|--------|
| val_ndcg_k giảm | < 0.7 | Kiểm tra data quality |
| train-val gap tăng | > 0.2 RMSE | Giảm model complexity |
| Feature importance thay đổi lớn | Top-3 khác run trước | Kiểm tra data distribution |
| zero_ratio labels | > 0.8 | Thiếu positive samples |

### Data drift detection

So sánh `feature_stats` giữa các lần train:
- Mean/std thay đổi > 50% → data distribution shifted
- zero_ratio thay đổi lớn → missing data pattern changed
- max value vượt xa cap → outlier pattern changed
