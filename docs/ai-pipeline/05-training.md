# 05 — Training

## Tổng quan

Bước Training huấn luyện model Gradient Boosted Decision Trees sử dụng scikit-learn, với early stopping dựa trên NDCG@10 trên validation set.

**File chính:** `training/trainer.py`

## Thuật toán: Gradient Boosting Regression

### Nguyên lý
Gradient Boosting xây dựng model bằng cách **cộng dồn** nhiều decision trees nhỏ (weak learners), mỗi tree học từ **residuals** (sai số) của ensemble trước đó.

```
prediction = bias + lr * tree_1(x) + lr * tree_2(x) + ... + lr * tree_n(x)
```

### Tại sao chọn GBDT cho ranking?
- Xử lý tốt features hỗn hợp (numeric + binary)
- Không cần feature scaling
- Robust với missing values
- Interpretable (feature importance)
- State-of-the-art cho tabular data và ranking tasks

## Data Split: Temporal Split

```python
def split_rows(self, rows: list[TrainingRow]) -> DatasetSplit:
    sorted_rows = sorted(rows, key=lambda r: r.created_utc)
    split_idx = int(len(sorted_rows) * (1 - 0.2))  # 80/20
    return DatasetSplit(sorted_rows[:split_idx], sorted_rows[split_idx:])
```

| Set | Tỷ lệ | Dữ liệu |
|-----|--------|---------|
| Train | 80% | Posts cũ nhất (theo thời gian) |
| Validation | 20% | Posts mới nhất (theo thời gian) |

### Tại sao Temporal Split thay vì Random Split?

**Random split** gây **temporal data leakage**:
- Post ngày 1/4 có thể ở validation
- Post ngày 30/4 có thể ở training
- Model "nhìn thấy tương lai" → overfit, metric ảo

**Temporal split** mô phỏng thực tế:
- Train trên dữ liệu quá khứ
- Evaluate trên dữ liệu tương lai
- Metric phản ánh đúng production performance

## Hyperparameters

```python
model = GradientBoostingRegressor(
    n_estimators=200,        # Số trees tối đa
    max_depth=3,             # Độ sâu mỗi tree (shallow = less overfit)
    min_samples_leaf=64,     # Minimum samples per leaf
    learning_rate=0.18,      # Shrinkage factor
    subsample=0.8,           # Row sampling (stochastic GB)
    random_state=42,         # Reproducibility
)
```

| Parameter | Value | Giải thích |
|-----------|-------|-----------|
| `n_estimators` | 200 | Upper bound — early stopping sẽ dừng sớm hơn |
| `max_depth` | 3 | Shallow trees → mỗi tree chỉ capture interactions đơn giản |
| `min_samples_leaf` | 64 | Tránh overfit trên leaf nodes quá nhỏ |
| `learning_rate` | 0.18 | Moderate — trade-off giữa convergence speed và generalization |
| `subsample` | 0.8 | 80% rows per tree — thêm randomness, giảm overfit |
| `random_state` | 42 | Đảm bảo reproducibility |

## Early Stopping (NDCG-based)

### Vấn đề với RMSE early stopping
RMSE đo lỗi regression tổng thể, nhưng ranking model cần **thứ tự đúng**, không cần giá trị chính xác. Model có RMSE thấp hơn chưa chắc rank tốt hơn.

### Giải pháp: Early stopping trên NDCG@10

```python
for i, (train_pred, val_pred) in enumerate(
    zip(model.staged_predict(X_train), model.staged_predict(X_val))
):
    val_ndcg = _ndcg(validation_rows, val_pred, k=10)

    if val_ndcg > best_val_ndcg:
        best_val_ndcg = val_ndcg
        best_n_estimators = i + 1
        rounds_no_improve = 0
    else:
        rounds_no_improve += 1
        if rounds_no_improve >= early_stopping_rounds:
            break
```

**Cơ chế:**
1. Train full model (200 trees)
2. Dùng `staged_predict` để evaluate tại mỗi iteration
3. Track best NDCG@10 trên validation
4. Nếu không improve sau `early_stopping_rounds` (default: 10) → dừng
5. Trim model về best checkpoint

## Data Validation (Pre-training)

```python
def _validate_data(X, y, name):
    if len(X) == 0:
        raise ValueError(f"{name} set is empty")
    if np.any(np.isnan(X)):
        nan_cols = np.where(np.any(np.isnan(X), axis=0))[0]
        raise ValueError(f"{name} set has NaN in columns: {nan_cols}")
    if np.any(np.isinf(X)):
        raise ValueError(f"{name} set has infinite values")
    if np.any(np.isnan(y)) or np.any(np.isinf(y)):
        raise ValueError(f"{name} labels contain NaN or Inf")
```

Fail fast nếu data không hợp lệ — không waste time training model lỗi.

## Feature Importance

Sau training, log feature importance (impurity-based):

```python
importances = model.feature_importances_
importance_ranking = sorted(zip(feature_names, importances), key=lambda x: x[1], reverse=True)
```

Output:
```
Feature importance (top 10):
  hot_score: 0.3245
  popularity: 0.2156
  upvote_count: 0.1432
  interaction_count_30d: 0.0987
  ...
```

**Mục đích:**
- Hiểu model đang dựa vào features nào
- Phát hiện features không hữu ích (importance = 0)
- Debug khi model performance giảm

## Model Export

Serialize sklearn model thành JSON format tương thích với scorer:

```python
def _export_model(model, feature_names, importances):
    tree_info = []

    # Bias tree (initial prediction = mean of labels)
    init_value = float(model.init_.constant_[0][0])
    tree_info.append({"shrinkage": 1.0, "tree_structure": {"leaf_value": init_value}})

    # Each boosting iteration
    for i in range(model.n_estimators_):
        tree = model.estimators_[i, 0].tree_
        tree_info.append({
            "shrinkage": model.learning_rate,
            "tree_structure": _tree_to_dict(tree, 0),
        })

    return {
        "objective": "regression",
        "average_output": False,
        "feature_names": feature_names,
        "tree_info": tree_info,
        "feature_importances": dict(zip(feature_names, importances.tolist())),
    }
```

### Tree node format
```json
{
  "split_feature": 4,
  "threshold": 2.5,
  "decision_type": "<=",
  "default_left": true,
  "left_child": { "leaf_value": 0.123 },
  "right_child": { "split_feature": 7, ... }
}
```

## CLI Arguments

| Argument | Default | Mô tả |
|----------|---------|--------|
| `--n-estimators` | 200 | Số trees tối đa |
| `--max-depth` | 3 | Độ sâu tree |
| `--min-samples-leaf` | 64 | Min samples per leaf |
| `--learning-rate` | 0.18 | Learning rate |
| `--early-stopping-rounds` | 10 | Patience cho early stopping |
| `--seed` | 42 | Random seed |

## Reproducibility

Đảm bảo kết quả giống nhau giữa các lần chạy:
1. `random_state=42` cho sklearn model
2. `seed=42` cho reservoir sampling
3. Temporal split deterministic (sort by timestamp)
4. Fixed dependency versions trong `pyproject.toml`
