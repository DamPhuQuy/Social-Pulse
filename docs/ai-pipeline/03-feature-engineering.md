# 03 — Feature Engineering

## Tổng quan

Feature Engineering chuyển đổi raw data (SubmissionRecord + AuthorAggregate + Interactions) thành **feature vectors** 19 chiều cho model. Đây là bước quyết định chất lượng model vì "garbage in, garbage out".

**File chính:** `training/feature_engineering.py`

## Feature Schema (19 features)

Thứ tự features được định nghĩa trong `shared/schema.py` — là **source of truth** cho cả training và inference.

### Post Features (indices 0–5)

| # | Feature | Type | Ý nghĩa | Cách tính |
|---|---------|------|----------|-----------|
| 0 | `content_length` | int | Độ dài nội dung | `title_length + body_length` |
| 1 | `has_multimedia` | binary | Có media không | Video, image, media embed |
| 2 | `is_share_post` | binary | Là bài chia sẻ | `num_crossposts > 0` hoặc có `crosspost_parent` |
| 3 | `post_age_hours` | float | Tuổi bài viết (giờ) | `(reference_utc - created_utc) / 3600` |
| 4 | `hot_score` | float | Reddit hot score | `sign * log10(score) + seconds / 45000` |
| 5 | `upvote_ratio` | float | Tỷ lệ upvote | Từ Reddit API, default 0.5 nếu thiếu |

### Author Features (indices 6–8)

| # | Feature | Type | Ý nghĩa | Cách tính |
|---|---------|------|----------|-----------|
| 6 | `author_seniority` | float | Thâm niên tác giả (năm) | `(post_created - author_created) / seconds_per_year` |
| 7 | `author_post_count` | float | Số bài đã đăng | Tích lũy từ AuthorAggregate |
| 8 | `author_engagement_rate` | float | Trung bình popularity | `cumulative_popularity / post_count` |

### Interaction Features (indices 9–12)

| # | Feature | Type | Ý nghĩa | Cách tính |
|---|---------|------|----------|-----------|
| 9 | `interaction_count_7d` | int | Tương tác 7 ngày gần | Đếm comments của viewer với author trong 7 ngày trước post |
| 10 | `interaction_count_30d` | int | Tương tác 30 ngày gần | Tương tự, window 30 ngày |
| 11 | `hours_since_last_interaction` | float | Giờ từ lần tương tác cuối | `(post_created - latest_interaction) / 3600`, default 999 |
| 12 | `affinity_score` | float | Mức độ gắn kết | `count_30d / viewer_total_interactions` |

### Engagement Metrics (indices 13–18)

| # | Feature | Type | Ý nghĩa | Cách tính |
|---|---------|------|----------|-----------|
| 13 | `upvote_count` | int | Số upvotes | `max(score, 0)` |
| 14 | `downvote_count` | int | Số downvotes | 0 (Reddit không expose) |
| 15 | `comment_count` | int | Số comments | Từ `num_comments` |
| 16 | `share_count` | int | Số lần share | Từ `num_crossposts` |
| 17 | `view_count` | int | Số lượt xem | 0 (Reddit không expose) |
| 18 | `popularity` | float | Tổng hợp popularity | `score + num_comments + num_crossposts` |

## Xây dựng Training Rows

### Positive samples (viewer đã tương tác với author)

Với mỗi post, tìm tất cả viewers đã comment bài của cùng author:

```python
for viewer, timestamps in author_interactors.items():
    interaction_feats = compute_interaction_features(timestamps, post_created_utc, viewer_total)
    rows.append(TrainingRow(post_id, merge(base, interaction_feats), log1p(popularity), created_utc))
```

**Label:** `log1p(popularity)` — log-transform để giảm ảnh hưởng của viral posts.

### Negative samples (viewer chưa tương tác với author)

```python
negative_viewers = find_negative_viewers(interactions, author, limit=3)
for _ in negative_viewers:
    rows.append(TrainingRow(post_id, merge(base, zero_interaction), label=0.0, created_utc))
```

**Label:** `0.0` — giả định không có affinity = không relevant.

**Tỷ lệ negative/positive:** 3:1 (configurable qua `_NEGATIVE_SAMPLES_PER_POST`).

### Fallback (không có interaction data)

Nếu post không có cả positive lẫn negative viewers:
```python
rows.append(TrainingRow(post_id, merge(base, zero_interaction), log1p(popularity), created_utc))
```

Giữ lại post với interaction features = 0, label = popularity gốc.

## Interaction Feature Computation

```python
def _compute_interaction_features(timestamps, post_created_utc, viewer_total):
    # Chỉ đếm interactions TRƯỚC thời điểm post được tạo
    for ts in timestamps:
        if seven_days_before <= ts < post_created_utc:
            count_7d += 1
        if thirty_days_before <= ts < post_created_utc:
            count_30d += 1
        if ts > latest and ts < post_created_utc:
            latest = ts

    hours_since = (post_created_utc - latest) / 3600 if latest > 0 else 999.0
    affinity = count_30d / viewer_total if viewer_total > 0 else 0.0
    return [count_7d, count_30d, hours_since, affinity]
```

**Quan trọng:** Chỉ sử dụng interactions **trước** thời điểm post — tránh data leakage.

## Label Strategy

```
label = log1p(score + num_comments + num_crossposts)
```

| Chiến lược | Lý do |
|------------|-------|
| `log1p` transform | Giảm skewness, viral posts (score=100k) không dominate |
| Popularity proxy | Kết hợp nhiều signals (votes + comments + shares) |
| Personalized via interactions | Positive rows có label > 0, negative rows có label = 0 |

## Default Values

Khi feature bị thiếu (null/missing):

| Feature | Default | Lý do |
|---------|---------|-------|
| Numeric features | `0.0` | Neutral value |
| `upvote_ratio` | `0.5` | Trung bình (50/50) |
| `hours_since_last_interaction` | `999.0` | "Rất lâu rồi" — cold start |

## Output

```python
@dataclass(frozen=True)
class TrainingDataset:
    rows: list[TrainingRow]       # Feature vectors + labels
    feature_stats: dict[str, Any] # Distribution statistics
```

Mỗi `TrainingRow`:
```python
@dataclass(frozen=True)
class TrainingRow:
    post_id: str           # Để group cho NDCG
    features: list[float]  # 19-dim vector
    label: float           # log1p(popularity) hoặc 0.0
    created_utc: float     # Để temporal split
```
