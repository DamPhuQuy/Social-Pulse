# 02 — Data Ingestion

## Tổng quan

Bước Data Ingestion đọc dữ liệu thô từ Pushshift Reddit archives (định dạng `.zst` — Zstandard compressed JSON lines), lọc bỏ dữ liệu không hợp lệ, và sampling để tạo tập dữ liệu huấn luyện.

**File chính:** `training/scanner.py`

## Nguồn dữ liệu

| File | Nội dung | Kích thước |
|------|----------|-----------|
| `RS_2019-04.zst` | Reddit Submissions (posts) tháng 4/2019 | ~5.5 GB |
| `RC_2019-04.zst` | Reddit Comments tháng 4/2019 | ~15.5 GB |

Mỗi dòng trong file là một JSON object chứa metadata của một submission hoặc comment.

## Quy trình xử lý

### 1. Streaming decompression

```python
class JsonLineReader:
    """Streams JSON lines from a .zst compressed file."""
    def __init__(self, path: Path):
        self._fh = open(path, "rb")
        self._dctx = zstd.ZstdDecompressor()
        self._reader = self._dctx.stream_reader(self._fh)
        self._lines = io.TextIOWrapper(self._text_stream, encoding="utf-8")
```

Dữ liệu được đọc **streaming** (không load toàn bộ vào RAM) nhờ `zstandard` library. Mỗi dòng được parse thành dict riêng lẻ.

### 2. Submission filtering

Mỗi submission đi qua `_preprocess_submission()` với các bộ lọc:

| Điều kiện lọc | Lý do |
|---------------|-------|
| `author` rỗng hoặc `[deleted]` / `AutoModerator` | Không có thông tin tác giả |
| `created_utc <= 0` | Timestamp không hợp lệ |
| Không có `title` lẫn `body` | Bài viết trống |
| `len(title) + len(body) < min_content_length` (default: 20) | Quá ngắn, không đủ signal |
| Không có `id` | Không thể track |

### 3. Text normalization

```python
def normalize_text(value: str | None) -> str:
    if value is None:
        return ""
    normalized = value.strip()
    if normalized.lower() in ("[deleted]", "[removed]", "null"):
        return ""
    return normalized
```

Xử lý các giá trị đặc biệt của Reddit: `[deleted]`, `[removed]`, `null`.

### 4. Reservoir Sampling

Khi `sample_size > 0`, sử dụng **Algorithm R** (reservoir sampling) để lấy mẫu ngẫu nhiên đều từ stream:

```python
if len(reservoir) < arguments.sample_size:
    reservoir.append(record)
else:
    idx = rng.randint(0, accepted - 1)
    if idx < arguments.sample_size:
        reservoir[idx] = record
```

**Đặc điểm:**
- Mỗi record có xác suất bằng nhau được chọn: `sample_size / total_accepted`
- Không cần biết trước tổng số records
- Seed cố định (`arguments.seed = 42`) đảm bảo reproducibility
- Nếu `sample_size = 0`: lấy tất cả records (không sampling)

### 5. Author Aggregation

Song song với scanning, tích lũy thống kê cho mỗi tác giả:

```python
class AuthorAggregate:
    _post_count: int
    _cumulative_popularity: float

    def increment(self, popularity: float) -> None:
        self._post_count += 1
        self._cumulative_popularity += popularity
```

Cung cấp `post_count` và `average_popularity` cho feature engineering.

### 6. Interaction Extraction (Comments)

Scan file comments để xây dựng **interaction graph** (viewer → author → timestamps):

```python
interactions[commenter][post_author].append(created_utc)
```

**Logic:**
1. Đọc mỗi comment
2. Tìm `link_id` → map về `post_id` → tìm `post_author`
3. Nếu commenter ≠ post_author → ghi nhận interaction
4. Lưu timestamp để tính recency features

**Bộ lọc comments:**
- Bỏ `[deleted]` authors
- Bỏ self-comments (commenter = post_author)
- Bỏ comments không link được về sampled posts
- Bỏ `created_utc <= 0`

## Output

### ScanResult
```python
@dataclass(frozen=True)
class ScanResult:
    sampled_posts: list[SubmissionRecord]      # Cleaned, sampled posts
    author_aggregates: dict[str, AuthorAggregate]  # Author stats
    scan_stats: dict[str, int]                 # Monitoring counters
```

### InteractionScanResult
```python
@dataclass(frozen=True)
class InteractionScanResult:
    interactions: dict[str, dict[str, list[float]]]  # viewer → author → [timestamps]
    stats: dict[str, int]
```

### SubmissionRecord (per post)
```python
@dataclass(frozen=True)
class SubmissionRecord:
    post_id: str
    author: str
    author_created_utc: float | None
    created_utc: float
    retrieved_on: float
    title_length: int
    body_length: int
    score: int
    num_comments: int
    num_crossposts: int
    has_multimedia: bool
    is_share_post: bool
    hot_score: float
    upvote_ratio: float
```

## Derived fields

### Multimedia detection
```python
def _detect_multimedia(payload: dict) -> bool:
    # True nếu: is_video, có media/secure_media,
    # thumbnail không phải default, hoặc URL kết thúc bằng media extension
```

### Share post detection
```python
def _detect_share_post(payload: dict) -> bool:
    return num_crossposts > 0 or crosspost_parent is not None
```

### Reddit Hot Score
```python
def _reddit_hot_score(score: int, created_utc: float) -> float:
    order = log10(max(abs(score), 1))
    sign = 1.0 if score > 0 else (-1.0 if score < 0 else 0.0)
    seconds = created_utc - REDDIT_EPOCH  # 1134028003
    return sign * order + seconds / 45000.0
```

Công thức hot score gốc của Reddit — kết hợp popularity và recency.

## Scan statistics (monitoring)

```json
{
  "submissions_scanned": 12000000,
  "submissions_filtered": 3500000,
  "submissions_accepted": 8500000,
  "reservoir_size": 100000
}
```

## CLI Arguments liên quan

| Argument | Default | Mô tả |
|----------|---------|--------|
| `--submissions` | (required) | Path tới RS_*.zst |
| `--comments` | (optional) | Path tới RC_*.zst |
| `--sample-size` | 0 (all) | Số lượng posts sampling |
| `--scan-limit-posts` | 0 (all) | Giới hạn posts scan |
| `--scan-limit-comments` | 0 (all) | Giới hạn comments scan |
| `--min-content-length` | 20 | Độ dài tối thiểu title+body |
| `--seed` | 42 | Random seed cho reservoir |
