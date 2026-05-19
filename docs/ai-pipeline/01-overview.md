# 01 — Tổng quan AI Pipeline

## Mục đích

AI Pipeline của Social Pulse thực hiện **xếp hạng bài viết cá nhân hóa** (personalized feed ranking). Hệ thống dự đoán mức độ phù hợp của mỗi bài viết đối với từng người dùng, dựa trên đặc trưng bài viết, tác giả, và lịch sử tương tác.

## Bài toán ML

| Thuộc tính | Giá trị |
|------------|---------|
| Loại bài toán | Learning-to-Rank (pointwise regression) |
| Mô hình | Gradient Boosted Decision Trees (scikit-learn) |
| Input | 19 features (post + author + interaction) |
| Output | Relevance score (float) |
| Label | `log1p(popularity)` — proxy cho mức độ hấp dẫn |
| Metric chính | NDCG@10 |

## Kiến trúc tổng thể

```
┌─────────────────────────────────────────────────────────────────┐
│                        TRAINING PIPELINE                         │
├─────────────┬──────────────┬──────────────┬────────────────────┤
│ Data        │ Feature      │ Preprocessing│ Model Training     │
│ Ingestion   │ Engineering  │              │ + Evaluation       │
│             │              │              │                    │
│ scanner.py  │ feature_     │ feature_     │ trainer.py         │
│             │ engineering  │ engineering  │                    │
│             │ .py          │ .py          │                    │
└──────┬──────┴──────┬───────┴──────┬───────┴─────────┬──────────┘
       │             │              │                 │
       ▼             ▼              ▼                 ▼
  .zst archives → SubmissionRecord → Feature Matrix → model.json
                                                         │
┌────────────────────────────────────────────────────────┼────────┐
│                     INFERENCE PIPELINE                  │        │
├──────────────┬─────────────────┬───────────────────────┤        │
│ Feature      │ Model Scoring   │ API Server            │        │
│ Vectorizer   │                 │                       │        │
│              │                 │                       │        │
│ vectorizer.py│ ranking_service │ server.py             │        │
│              │ .py + scorer.py │ (FastAPI)             │        │
└──────────────┴─────────────────┴───────────────────────┘        │
       ▲                                                          │
       │              model.json ◄────────────────────────────────┘
       │
  Spring Boot Backend ──POST /api/ranking/predict──▶ AI Service
```

## Cấu trúc thư mục

```
ai_pipeline/
├── pyproject.toml          # Dependencies & scripts
├── server.py               # FastAPI inference server
├── Dockerfile              # Container build
├── __init__.py
├── shared/                 # Shared giữa training & inference
│   ├── schema.py           # Feature schema (19 features, defaults)
│   ├── model.py            # Model artifact data types + parser
│   └── scorer.py           # Tree-traversal scorer
├── training/               # Offline training pipeline
│   ├── main.py             # CLI entry point
│   ├── arguments.py        # Hyperparameter config
│   ├── scanner.py          # Data ingestion (.zst reader)
│   ├── feature_engineering.py  # Feature construction + preprocessing
│   ├── trainer.py          # sklearn GBR training
│   ├── pipeline.py         # Orchestrator
│   ├── types.py            # Data types
│   └── json_support.py     # JSON/zstd utilities
├── inference/              # Online inference
│   ├── vectorizer.py       # Feature vector construction
│   └── ranking_service.py  # Model loading + scoring
├── model/                  # Model artifacts (output)
│   └── model.json
└── data/                   # Training data (input)
    ├── RS_2019-04.zst      # Reddit submissions
    └── RC_2019-04.zst      # Reddit comments
```

## Luồng xử lý chính

### Training (offline)
1. **Data Ingestion** → Đọc .zst archives, lọc & sampling
2. **Feature Engineering** → Xây dựng 19 features từ raw data
3. **Preprocessing** → Outlier capping + log-transform
4. **Training** → sklearn GradientBoostingRegressor + early stopping
5. **Evaluation** → RMSE, MAE, NDCG@10 + feature importance
6. **Export** → Serialize model thành JSON artifact

### Inference (online)
1. Backend gửi request với features của candidate posts
2. Vectorizer chuyển đổi features (apply cùng transforms)
3. Scorer traverse decision trees → relevance score
4. Trả về ranked scores cho backend

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Python 3.12 |
| ML Framework | scikit-learn 1.5+ |
| Numerical | NumPy 1.26+ |
| Data Format | Zstandard compressed JSON lines |
| API Server | FastAPI + Uvicorn |
| Container | Docker (Alpine-based) |
| Model Format | Custom JSON (tree serialization) |

## Chạy pipeline

```bash
# Training
cd ai_pipeline
uv run train --submissions data/RS_2019-04.zst \
             --comments data/RC_2019-04.zst \
             --output model/model.json \
             --metrics-output model/metrics.json

# Inference server
uvicorn ai_pipeline.server:app --host 0.0.0.0 --port 8000
```
