# Social Pulse AI Feed Ranking System

## Overview

This directory contains the implementation guide, code templates, and resources for building an AI-powered feed ranking system for Social Pulse, based on the research paper "Ranking News Feed Updates on Social Media: A Comparative Study of Supervised Models."

## Research Foundation

**Paper Results**: 82-84% accuracy using Gradient Boosting and Random Forest on 26,180 tweets with 13 features.

**Key Findings**:
- Top 5 features account for 70% of model importance
- Interaction rate is the most important feature (25-30%)
- Keywords relevance is second most important (18-22%)
- Ensemble methods (GB + RF) outperform single models

## Project Structure

```
ai/
├── README.md                           # This file
├── ANALYSIS_BREAKDOWN.md               # Research paper analysis (provided)
├── IMPLEMENTATION_GUIDE.md             # Complete implementation roadmap
├── FEATURE_EXTRACTION_PLAN.md          # Detailed feature extraction guide
├── app/
│   ├── data/
│   │   └── all_data.csv               # Sample dataset from research
│   ├── train_model.py                 # Model training script (to be created)
│   └── prediction_service.py          # Flask API for predictions (to be created)
├── models/                            # Trained models directory (to be created)
├── database/
│   └── V1__create_training_data_table.sql  # Database migration
└── notebooks/                         # Jupyter notebooks for analysis (optional)
```

## Documents Created

### 1. IMPLEMENTATION_GUIDE.md
**Purpose**: Complete end-to-end implementation strategy

**Contents**:
- Feature mapping from research paper to Social Pulse domain
- 13 core features + 13 additional features (26 total)
- 5-phase implementation roadmap (6-8 weeks)
- Python training scripts with hyperparameter tuning
- Flask prediction API
- Java backend integration
- A/B testing framework
- Success metrics and KPIs

**Key Sections**:
- ✅ Feature mapping table (research → your domain)
- ✅ Data requirements (MVP: 7.5K samples, Production: 200K+)
- ✅ Model selection recommendations (Random Forest vs Gradient Boosting)
- ✅ Cold start strategy for new users
- ✅ Timeline and milestones

### 2. FEATURE_EXTRACTION_PLAN.md
**Purpose**: Detailed implementation of feature extraction services

**Contents**:
- `ContentAnalysisService.java` - Extract keywords, hashtags, mentions, URLs
- `UserInterestProfileService.java` - Build user interest profiles
- Enhanced `FeatureExtractionService.java` - Extract all 26 features
- New DTO classes for complete feature set
- Unit testing strategy
- Performance optimization (caching, batching, indexing)

**Key Components**:
- ✅ Regex patterns for content parsing
- ✅ TF-IDF keyword weighting
- ✅ Cosine similarity for relevance scoring
- ✅ Time-decay for recency weighting
- ✅ Redis caching strategy

### 3. V1__create_training_data_table.sql
**Purpose**: Database schema for training data collection

**Contents**:
- `feed_training_data` table (26 features + metadata)
- `user_interest_profiles` table (cached profiles)
- `feed_impressions` table (impression tracking)
- `training_data_summary` materialized view
- Helper functions for interaction rate calculation
- Indexes for query optimization

**Key Features**:
- ✅ All 26 feature columns with proper types
- ✅ Constraints for data validation
- ✅ Indexes for efficient querying
- ✅ Materialized view for daily summaries
- ✅ PostgreSQL functions for calculations

### 4. TrainingDataCollectionService.java
**Purpose**: Service for collecting training data in production

**Contents**:
- Record impressions when users see posts
- Record interactions when users engage
- Extract features at impression time
- Generate negative samples (non-interactions)
- Export training data to CSV
- Training data statistics

**Key Methods**:
- `recordImpression()` - Track when user sees a post
- `recordInteraction()` - Track when user engages
- `generateNegativeSamples()` - Create negative training samples
- `exportTrainingData()` - Export for model training
- `getTrainingDataStats()` - Monitor data collection

## Feature Set Summary

### Core Features (13 from research paper)

#### Content Features (4)
1. **keywords_relevance** (0-1000) - Keyword matching with user interests
2. **hashtags_relevance** (0-1000) - Hashtag matching with user interests
3. **mentions_relevance** (0/1) - Binary: user mentioned in post
4. **content_length** (0-5000) - Character count

#### Author Features (5)
5. **interaction_rate** (0-1) - Historical user-author interaction rate ⭐ **MOST IMPORTANT**
6. **mention_count** (0-1000) - Times author mentioned user
7. **followers_followings_ratio** (0-1M) - Author influence metric
8. **author_seniority** (0-20) - Account age in years
9. **author_engagement_rate** (0-1) - Author's average engagement

#### Metadata Features (3)
10. **has_hashtags** (0/1) - Binary: contains hashtags
11. **has_url** (0/1) - Binary: contains URLs
12. **has_multimedia** (0/1) - Binary: has image/video

#### Engagement Features (1)
13. **popularity** (0-∞) - Total engagement count ⭐ **3RD MOST IMPORTANT**

### Additional Features (13 from your domain)

14. **follows** (0/1) - User follows author
15. **interaction_count_7d** - Interactions in last 7 days
16. **interaction_count_30d** - Interactions in last 30 days
17. **hours_since_last_interaction** - Recency metric
18. **affinity_score** - Time-decayed weighted score
19. **author_follower_count** - Raw follower count
20. **author_following_count** - Raw following count
21. **author_post_count** - Total posts by author
22. **upvote_count** - Upvotes at impression time
23. **downvote_count** - Downvotes at impression time
24. **comment_count** - Comments at impression time
25. **share_count** - Shares at impression time
26. **view_count** - Views at impression time

## Implementation Phases

### Phase 1: Core Feature Extraction (Week 1-2)
**Status**: 📝 Code templates provided

**Tasks**:
- [ ] Implement `ContentAnalysisService.java`
- [ ] Implement `UserInterestProfileService.java`
- [ ] Create new DTO classes (ContentFeatures, AuthorFeatures, etc.)
- [ ] Update `FeatureExtractionService.java`
- [ ] Write unit tests

**Deliverables**: Working feature extraction for top 5 features

### Phase 2: Data Collection & Storage (Week 2-3)
**Status**: 📝 SQL migration provided, Java service provided

**Tasks**:
- [ ] Run database migration (V1__create_training_data_table.sql)
- [ ] Deploy `TrainingDataCollectionService.java`
- [ ] Integrate impression tracking in `FeedController`
- [ ] Integrate interaction tracking in `PostController`
- [ ] Set up scheduled job for negative sample generation

**Deliverables**: Training data collection pipeline running in production

### Phase 3: Model Training Pipeline (Week 3-4)
**Status**: 📝 Python script template provided in IMPLEMENTATION_GUIDE.md

**Tasks**:
- [ ] Create Python environment (requirements.txt)
- [ ] Implement `train_model.py` script
- [ ] Export training data from database to CSV
- [ ] Train Gradient Boosting model
- [ ] Train Random Forest model
- [ ] Evaluate models and save best performers

**Deliverables**: Trained models with 75%+ accuracy

### Phase 4: Model Serving (Week 4-5)
**Status**: 📝 Flask API template provided in IMPLEMENTATION_GUIDE.md

**Tasks**:
- [ ] Implement `prediction_service.py` (Flask API)
- [ ] Deploy Flask service (Docker container)
- [ ] Implement `AiRankingService.java` in backend
- [ ] Integrate AI ranking with feed generation
- [ ] Add fallback to hot score ranking

**Deliverables**: AI-powered feed ranking in production

### Phase 5: A/B Testing & Monitoring (Week 5-6)
**Status**: 📝 Framework design provided in IMPLEMENTATION_GUIDE.md

**Tasks**:
- [ ] Implement `FeedExperimentService.java`
- [ ] Implement `FeedMetricsService.java`
- [ ] Deploy to 30% of users
- [ ] Monitor engagement metrics
- [ ] Compare AI ranking vs baseline

**Deliverables**: A/B test results and performance metrics

## Current Gaps in Your Codebase

### Critical (Must Fix)
1. ❌ **Content Analysis** - No keyword/hashtag extraction
2. ❌ **User Interest Profiles** - No historical preference tracking
3. ❌ **Training Data Collection** - No impression/interaction tracking
4. ⚠️ **BehaviorFeaturesExtractionService** - Groups by postId instead of authorId (bug)

### Important (Should Fix)
5. ⚠️ **User Table** - Missing `createdAt` field for seniority calculation
6. ⚠️ **Author Stats** - No aggregated follower/following counts
7. ⚠️ **Mention Detection** - No username extraction from content

### Nice to Have (Can Add Later)
8. ⚠️ **Listed Count** - No credibility metric (can skip for MVP)
9. ⚠️ **Temporal Features** - No time-of-day patterns
10. ⚠️ **Network Features** - No mutual friends analysis

## Quick Start Guide

### Step 1: Review Documentation
```bash
# Read the implementation guide
cat IMPLEMENTATION_GUIDE.md

# Read the feature extraction plan
cat FEATURE_EXTRACTION_PLAN.md
```

### Step 2: Run Database Migration
```bash
# Apply the SQL migration
psql -U your_user -d social_pulse -f database/V1__create_training_data_table.sql
```

### Step 3: Implement Feature Extraction
```bash
# Copy the service templates from FEATURE_EXTRACTION_PLAN.md
# Implement ContentAnalysisService.java
# Implement UserInterestProfileService.java
# Create new DTO classes
```

### Step 4: Deploy Training Data Collection
```bash
# Copy TrainingDataCollectionService.java to your project
# Integrate with FeedController and PostController
# Start collecting data
```

### Step 5: Collect Training Data (2-4 weeks)
```bash
# Let the system collect 10K-20K training samples
# Monitor with: SELECT * FROM training_data_summary;
```

### Step 6: Train Models
```bash
cd ai/
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate
pip install pandas numpy scikit-learn xgboost joblib flask

# Export training data
psql -U your_user -d social_pulse -c "COPY (SELECT * FROM feed_training_data) TO '/tmp/training_data.csv' CSV HEADER;"

# Train models
python train_model.py
```

### Step 7: Deploy Prediction Service
```bash
# Start Flask API
python prediction_service.py

# Test prediction endpoint
curl -X POST http://localhost:5000/predict \
  -H "Content-Type: application/json" \
  -d '{"features": [...]}'
```

### Step 8: Integrate with Backend
```java
// Implement AiRankingService.java
// Update FeedController to use AI ranking
// Deploy to production
```

## Expected Results

### Model Performance
- **Accuracy**: 80-84% (matching research paper)
- **F1-Score**: 80-84%
- **Training Time**: 10-20 seconds per model
- **Prediction Time**: <100ms for 100 posts

### Business Impact
- **Engagement Rate**: +15-25% increase
- **Session Duration**: +20-30% increase
- **User Retention**: +10-15% increase
- **Posts per Session**: +25-35% increase

### Data Requirements
- **MVP**: 7,500-20,000 training samples (50-100 users, 2-4 weeks)
- **Production**: 200,000+ training samples (500+ users, 2-3 months)

## Monitoring & Maintenance

### Daily Checks
- Training data collection rate
- Positive/negative sample ratio (target: 35-40% positive)
- Feature extraction errors
- Prediction service uptime

### Weekly Tasks
- Refresh materialized view: `SELECT refresh_training_summary();`
- Generate negative samples: `TrainingDataCollectionService.generateNegativeSamples(24)`
- Review A/B test metrics
- Check model prediction latency

### Monthly Tasks
- Retrain models with new data
- Update user interest profiles
- Analyze feature importance
- Optimize slow queries

## Troubleshooting

### Low Positive Rate (<20%)
- Users not engaging with content
- Interaction tracking not working
- Need to adjust positive event definition

### High Prediction Latency (>200ms)
- Feature extraction too slow
- Need to add caching
- Batch predictions instead of one-by-one

### Low Model Accuracy (<70%)
- Insufficient training data
- Feature extraction bugs
- Class imbalance too severe

### Missing Features
- Check user interest profile cache
- Verify behavior tracking is working
- Ensure post content is not null

## Resources

### Research Paper
- Title: "Ranking News Feed Updates on Social Media: A Comparative Study of Supervised Models"
- Dataset: 26,180 tweets, 46 users, 10 months
- Best Model: Gradient Boosting (82-84% accuracy)

### Documentation
- [IMPLEMENTATION_GUIDE.md](./IMPLEMENTATION_GUIDE.md) - Complete implementation strategy
- [FEATURE_EXTRACTION_PLAN.md](./FEATURE_EXTRACTION_PLAN.md) - Feature extraction details
- [ANALYSIS_BREAKDOWN.md](./ANALYSIS_BREAKDOWN.md) - Research paper analysis

### Code Templates
- `ContentAnalysisService.java` - In FEATURE_EXTRACTION_PLAN.md
- `UserInterestProfileService.java` - In FEATURE_EXTRACTION_PLAN.md
- `TrainingDataCollectionService.java` - In backend/src/.../feed/application/service/
- `train_model.py` - In IMPLEMENTATION_GUIDE.md
- `prediction_service.py` - In IMPLEMENTATION_GUIDE.md

## Next Steps

1. ✅ **Review all documentation** - Understand the complete system
2. ❌ **Implement ContentAnalysisService** - Start with content features
3. ❌ **Run database migration** - Set up training data tables
4. ❌ **Deploy training data collection** - Start collecting samples
5. ❌ **Wait 2-4 weeks** - Collect 10K+ training samples
6. ❌ **Train initial models** - Get to 75%+ accuracy
7. ❌ **Deploy prediction service** - Integrate with backend
8. ❌ **Run A/B test** - Measure impact on engagement

## Support

For questions or issues:
1. Review the implementation guide
2. Check the feature extraction plan
3. Refer to the research paper analysis
4. Debug with training data statistics

---

**Created**: 2026-05-01  
**Last Updated**: 2026-05-01  
**Status**: 📝 Documentation Complete, Implementation Pending  
**Estimated Timeline**: 6-8 weeks to production-ready AI ranking
