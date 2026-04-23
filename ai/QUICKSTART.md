# Production Feed Ranking System - Quick Start

## 🚀 Quick Start (5 minutes)

### 1. Install dependencies
```bash
cd ai
pip install -r requirements.txt
```

### 2. Generate training data
```bash
cd app
python data/generate_realistic_data.py
```

### 3. Train model
```bash
python training/train_ranker.py
```

### 4. Start AI service
```bash
cd ..
uvicorn app.main:app --host 0.0.0.0 --port 8001 --reload
```

### 5. Test
```bash
# Health check
curl http://localhost:8001/health

# Model info
curl http://localhost:8001/model/info
```

---

## 📁 Project Structure

```
ai/
├── app/
│   ├── data/
│   │   ├── data_schema.py              # Data models
│   │   ├── generate_realistic_data.py  # Generate training data
│   │   └── feature_engineering.py      # Feature extraction
│   ├── models/
│   │   ├── ranker.py                   # Two-stage ranking pipeline
│   │   ├── ranking_model.txt           # Trained LightGBM model
│   │   └── model_metadata.json         # Model metadata
│   ├── training/
│   │   └── train_ranker.py             # Training pipeline
│   └── main.py                         # FastAPI service
├── requirements.txt
└── README_PRODUCTION.md
```

---

## 🔑 Key Differences from Old System

### Old System (WRONG)
```python
# ❌ Synthetic target
df['engagement_score'] = 0.3 * upvotes + 0.2 * comments + ...

# ❌ Regression model
model = GradientBoostingRegressor()

# ❌ No relationship features
features = ['upvote_count', 'author_follower_count']

# ❌ Normal distribution
upvotes = np.random.poisson(50)
```

### New System (CORRECT)
```python
# ✅ Real user behavior simulation
engagement_prob = base_rate
if user_follows_author:
    engagement_prob *= 10.0
engaged = np.random.random() < engagement_prob

# ✅ Ranking model
model = lgb.LGBMRanker(objective='lambdarank')

# ✅ Relationship features
features = ['follows', 'affinity_score', 'topic_affinity', ...]

# ✅ Power-law distribution
upvotes = np.random.zipf(a=2.0)
```

---

## 📊 Expected Results

After training, you should see:

```
Validation Metrics:
  ndcg@5: 0.82
  ndcg@10: 0.81
  ndcg@20: 0.80
  map: 0.69

Top Features:
  1. affinity_score
  2. follows
  3. topic_affinity
  4. interaction_count_7d
  5. recency_score
```

---

## 🔧 Configuration

### Spring Boot (application.yml)
```yaml
ai:
  service:
    url: http://localhost:8001
```

### Environment Variables (.env)
```bash
AI_SERVICE_URL=http://localhost:8001
MODEL_PATH=./app/models/ranking_model.txt
```

---

## 🧪 Testing

### Test AI Service
```bash
# Health check
curl http://localhost:8001/health

# Model info
curl http://localhost:8001/model/info

# Feature importance
curl http://localhost:8001/model/feature-importance
```

### Test Feed Endpoint
```bash
curl -H "Authorization: Bearer <token>" \
  "http://localhost:8080/api/v1/feed?page=0&size=20"
```

---

## 📈 Monitoring

Key metrics to track:
- NDCG@10 (target: >0.80)
- User engagement rate (target: >8%)
- API latency (target: <200ms)
- Cache hit rate (target: >80%)

---

## 🐛 Troubleshooting

### Model not loading
```bash
# Check if model exists
ls app/models/ranking_model.txt

# If not, train model
python app/training/train_ranker.py
```

### Low NDCG scores
- Check if relationship features are populated
- Verify data distribution (should be power-law)
- Increase n_estimators in training config

### Slow inference
- Enable candidate generation (Stage 1)
- Reduce n_candidates from 500 to 200
- Add Redis caching

---

## 📚 Documentation

- Full documentation: `README_PRODUCTION.md`
- Architecture diagram: See README_PRODUCTION.md
- API docs: http://localhost:8001/docs (when service is running)

---

## 🎯 Next Steps

1. ✅ Train model with realistic data
2. ✅ Deploy FastAPI service
3. ✅ Integrate with Spring Boot
4. ⏳ Collect real production data
5. ⏳ Retrain with production data
6. ⏳ A/B test old vs new ranking
7. ⏳ Monitor engagement metrics

---

## 💡 Tips

- **Relationship features are critical** - they account for 60% of model performance
- **Use power-law distributions** - real social media data is heavily skewed
- **Optimize NDCG, not MSE** - ranking is about relative ordering
- **Two-stage pipeline** - candidate generation + ML ranking
- **Retrain frequently** - user preferences change over time

---

## 🆘 Support

For issues or questions:
1. Check `README_PRODUCTION.md` for detailed explanations
2. Review training logs for errors
3. Verify data generation completed successfully
4. Check FastAPI logs: `uvicorn app.main:app --log-level debug`
