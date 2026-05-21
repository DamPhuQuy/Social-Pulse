# 08 — Deployment

## Tổng quan

AI Pipeline được deploy dưới dạng Docker container, phục vụ inference qua HTTP API. Training chạy offline (CLI) và output model artifact được mount vào inference container.

## Docker

### Dockerfile

```dockerfile
FROM python:3.12-alpine

WORKDIR /app

RUN pip install --no-cache-dir uv

COPY pyproject.toml .
RUN uv pip install --system --no-cache -e .

COPY . .

EXPOSE 8000

CMD ["uvicorn", "ai_pipeline.server:app", "--host", "0.0.0.0", "--port", "8000"]
```

**Đặc điểm:**
- Alpine-based → image nhỏ (~200MB)
- `uv` package manager → install nhanh
- Model file (`model/model.json`) được COPY cùng source

### Build & Run

```bash
# Build
docker build -t social-pulse-ai .

# Run
docker run -p 8001:8000 \
  -e AI_PIPELINE_ENABLED=true \
  -e AI_PIPELINE_MODEL_LOCATION=/app/model/model.json \
  social-pulse-ai
```

## Docker Compose Integration

Trong `docker-compose.yaml` của project:

```yaml
services:
  ai-pipeline:
    build: ./ai_pipeline
    ports:
      - "8001:8000"
    environment:
      - AI_PIPELINE_ENABLED=true
      - AI_PIPELINE_MODEL_LOCATION=/app/model/model.json
      - AI_PIPELINE_FEATURE_SCHEMA_VERSION=v1
      - AI_PIPELINE_INFERENCE_DEVICE=cpu
    healthcheck:
      test: ["CMD-SHELL", "python -c \"import urllib.request; urllib.request.urlopen('http://localhost:8000/health', timeout=2).read()\""]
      interval: 30s
      timeout: 10s
      retries: 3

  backend:
    environment:
      AI_PIPELINE_BASE_URL: http://ai-pipeline:8000
    depends_on:
      ai-pipeline:
        condition: service_healthy
```

## Kết nối với Backend

Spring Boot backend gọi AI service qua HTTP:

```
Backend (Java) ──POST /api/ranking/predict──▶ AI Service (Python:8000)
```

**Flow:**
1. User request feed → Backend lấy candidate posts từ DB
2. Backend tính features cho mỗi post (content, author, interactions)
3. Backend gửi batch request tới AI service
4. AI service trả về scores
5. Backend sort posts theo score → trả về feed

**Fallback:** Nếu AI service unavailable → Backend dùng deterministic ranking (hot_score).

## Training Workflow

### Chạy training

```bash
cd ai_pipeline

# Full training (default args)
uv run train

# Custom args
uv run train \
  --submissions data/RS_2019-04.zst \
  --comments data/RC_2019-04.zst \
  --output model/model.json \
  --metrics-output model/metrics.json \
  --plots-output-dir model/plots \
  --sample-size 100000 \
  --n-estimators 1200 \
  --learning-rate 0.05 \
  --device cuda \
  --n-jobs 0 \
  --seed 42
```

### Output files

```
model/
├── model.json      # Metadata artifact
├── model.ubj       # XGBoost booster sidecar
├── metrics.json    # Training metrics + feature stats
└── plots/
    ├── feature_importance.png
    ├── label_distribution.png
    └── training_curves.png
```

### Retrain cycle

```
1. Thu thập data mới (hoặc dùng data hiện có)
2. Chạy training pipeline
3. Kiểm tra metrics (NDCG@10 >= threshold)
4. Replace model.json
5. Restart AI service (hoặc hot-reload)
```

## Environment Variables

| Variable | Default | Production | Mô tả |
|----------|---------|-----------|--------|
| `AI_PIPELINE_ENABLED` | `true` | `true` | Kill switch |
| `AI_PIPELINE_MODEL_LOCATION` | `ai_pipeline/model/model.json` | `/app/model/model.json` | Model path |
| `AI_PIPELINE_FEATURE_SCHEMA_VERSION` | `v1` | `v1` | Schema compatibility |
| `AI_PIPELINE_INFERENCE_DEVICE` | `cpu` | `cpu` | XGBoost inference device |

## Health Check

```
GET /health → {"status": "ok"}
```

Dùng cho:
- Docker healthcheck
- Load balancer health probe
- Kubernetes liveness/readiness probe

## Model Artifact Versioning

### Artifact structure

```json
{
  "artifact_version": "1",
  "feature_schema_version": "v1",
  "training_dataset": "pushshift_reddit_apr2019",
  "trained_at": "2024-01-15T10:30:00+00:00",
  "label_strategy": "log_popularity_proxy_personalized",
  "training_summary": { ... },
  "model_dump": { ... }
}
```

### Schema compatibility

- `feature_schema_version` phải match giữa model và inference service
- Nếu thay đổi feature set → bump version → deploy model + service cùng lúc
- Backward incompatible changes cần coordinated deployment

## Monitoring

### Metrics cần theo dõi

| Metric | Source | Alert threshold |
|--------|--------|----------------|
| Response latency (p99) | API server | > 100ms |
| Error rate | API server | > 1% |
| Model load failures | Logs | Any |
| Schema mismatch warnings | Logs | Any |
| Empty responses ratio | API server | > 10% |

### Logs

```
INFO: Loaded model from model.json: 45 trees, objective=regression, schema=v1
WARNING: Model not found at /app/model/model.json
WARNING: Schema mismatch: expected=v1, actual=v2
```

## Scaling

### Horizontal scaling
- Stateless service → scale bằng cách thêm replicas
- Model loaded in-memory per instance
- No shared state giữa instances

### Resource requirements

| Resource | Minimum | Recommended |
|----------|---------|-------------|
| CPU | 0.5 core | 1 core |
| Memory | 256 MB | 512 MB |
| Disk | 50 MB | 100 MB |

### Bottlenecks
- CPU-bound (tree traversal)
- Không I/O bound (model in-memory)
- Linear scaling với số posts per request

## Security

- Không expose trực tiếp ra internet
- Chỉ accessible từ backend service (internal network)
- Không xử lý user data trực tiếp (chỉ nhận features đã tính)
- Không lưu trữ request/response data
