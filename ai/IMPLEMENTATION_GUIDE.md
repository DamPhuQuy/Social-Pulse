# Social Pulse AI Feed Ranking - Implementation Guide

## Executive Summary

This document maps the 13 features from the research paper "Ranking News Feed Updates on Social Media" to your Social Pulse domain and provides a complete implementation strategy for AI-powered feed ranking.

**Research Context**: The paper achieved **82-84% accuracy** using Gradient Boosting and Random Forest on 26,180 tweets with 13 features across 4 categories.

**Your Current State**: You have partial infrastructure in place with:
- Basic feature extraction services
- Behavior tracking system
- Post, Follow, and Feed domains
- AI DTO structures prepared

---

## Feature Mapping: Research Paper → Social Pulse

### 1. Content-Based Features (4 features)

| Research Feature | Social Pulse Equivalent | Data Source | Implementation Status |
|-----------------|------------------------|-------------|---------------------|
| **Keywords_relevance** (0-412) | Content similarity score based on user's historical interests | `Post.content` + `UserBehavior` history | ❌ **MISSING** - Need NLP/keyword extraction |
| **Hashtags_relevance** (0-397) | Hashtag matching score | Extract from `Post.content` | ❌ **MISSING** - Need hashtag parser |
| **Mentions_relevance** (0/1) | Binary: post mentions the viewer | Parse `Post.content` for @mentions | ❌ **MISSING** - Need mention detection |
| **Length** (0-140) | Character count of post content | `Post.content.length()` | ✅ **EASY** - Direct field access |

**Implementation Priority**: HIGH
- Keywords_relevance is the 2nd most important feature (18-22% importance)
- Hashtags_relevance is 4th most important (10-12% importance)

### 2. Author-Based Features (5 features)

| Research Feature | Social Pulse Equivalent | Data Source | Implementation Status |
|-----------------|------------------------|-------------|---------------------|
| **Interaction_rate** (0.0-1.0) | Historical interaction rate between viewer and author | `UserBehavior` + `Follow` | ✅ **PARTIAL** - In `BehaviorFeaturesExtractionService` |
| **Mention_count** (0-108) | Times author mentioned viewer | `Post.content` history | ❌ **MISSING** - Need historical mention tracking |
| **Followers_Followings** (0-16M) | Author's follower/following ratio (influence) | `Follow` table aggregation | ⚠️ **PARTIAL** - Need to calculate ratio |
| **Seniority** (0-11 years) | Author account age in years | `User.createdAt` | ⚠️ **NEED USER TABLE** |
| **Listed_count** (0-228K) | Author credibility metric | Custom metric or skip | ⚠️ **OPTIONAL** - Not critical for MVP |

**Implementation Priority**: CRITICAL
- Interaction_rate is the #1 most important feature (25-30% importance)
- Followers_Followings is 5th most important (8-10% importance)

### 3. Tweet Metadata Features (3 features)

| Research Feature | Social Pulse Equivalent | Data Source | Implementation Status |
|-----------------|------------------------|-------------|---------------------|
| **Hashtags** (0/1) | Binary: post contains hashtags | Parse `Post.content` | ❌ **MISSING** - Need hashtag detection |
| **URL** (0/1) | Binary: post contains URLs | Parse `Post.content` | ❌ **MISSING** - Need URL detection |
| **Multimedia** (0/1) | Binary: post has image/video | `Post.imageUrl != null` | ✅ **EASY** - Direct field check |

**Implementation Priority**: MEDIUM
- These features have lower importance (2-4% each)
- Easy to implement with regex patterns

### 4. Social Engagement Features (1 feature)

| Research Feature | Social Pulse Equivalent | Data Source | Implementation Status |
|-----------------|------------------------|-------------|---------------------|
| **Popularity** (0-2.7M) | Total engagement count | `upvoteCount + downvoteCount + cmtCount + shareCount + viewCount` | ✅ **READY** - Already in `Post` model |

**Implementation Priority**: HIGH
- 3rd most important feature (12-15% importance)
- Already available in your domain

---

## Enhanced Feature Set for Social Pulse

Beyond the 13 research features, your domain supports additional signals:

### Additional Features Available

| Feature | Source | Benefit | Priority |
|---------|--------|---------|----------|
| **Recency Score** | `Post.createdAt` | Time decay for fresh content | ✅ Already implemented |
| **Hot Score** | `Post.hotScore` | Reddit-style ranking | ✅ Already implemented |
| **Toxic Score** | `Post.toxicScore` | Content safety filter | ✅ Already implemented |
| **Dwell Time** | `UserBehavior.dwellTimeSeconds` | Engagement quality signal | ⚠️ Need aggregation |
| **Position Bias** | `UserBehavior.position` | Correct for display position | ⚠️ Need analysis |
| **Skip Rate** | `EventType.SKIP` count | Negative signal | ⚠️ Need calculation |
| **Hide/Report Rate** | `EventType.HIDE/REPORT` | Strong negative signal | ⚠️ Need calculation |
| **Follow After View** | `EventType.FOLLOW` after `IMPRESSION` | Strong positive signal | ⚠️ Need sequence analysis |

---

## Complete Feature Schema for AI Model

### Input Features (26 total - expanded from research's 13)

#### A. Content Features (7 features)
```java
1.  keywordsRelevance: Double (0-1000)      // Cosine similarity with user interests
2.  hashtagsRelevance: Double (0-1000)      // Hashtag overlap score
3.  mentionsRelevance: Integer (0/1)        // Binary: viewer mentioned
4.  contentLength: Integer (0-5000)         // Character count
5.  hasHashtags: Integer (0/1)              // Binary: contains hashtags
6.  hasUrl: Integer (0/1)                   // Binary: contains URLs
7.  hasMultimedia: Integer (0/1)            // Binary: has image/video
```

#### B. Author Features (8 features)
```java
8.  interactionRate: Double (0.0-1.0)       // Historical viewer-author interaction rate
9.  mentionCount: Integer (0-1000)          // Times author mentioned viewer
10. followersFollowingsRatio: Double (0-1M) // Author influence metric
11. authorSeniority: Double (0-20)          // Account age in years
12. authorFollowerCount: Integer            // Raw follower count
13. authorFollowingCount: Integer           // Raw following count
14. authorPostCount: Integer                // Total posts by author
15. authorEngagementRate: Double (0-1)      // Author's avg engagement rate
```

#### C. Relationship Features (5 features)
```java
16. follows: Integer (0/1)                  // Binary: viewer follows author
17. interactionCount7d: Integer             // Interactions in last 7 days
18. interactionCount30d: Integer            // Interactions in last 30 days
19. hoursSinceLastInteraction: Double       // Recency of last interaction
20. affinityScore: Double                   // Weighted affinity score
```

#### D. Post Engagement Features (6 features)
```java
21. popularity: Long                        // Total engagement count
22. upvoteCount: Long                       // Upvotes
23. downvoteCount: Long                     // Downvotes
24. commentCount: Long                      // Comments
25. shareCount: Long                        // Shares
26. viewCount: Long                         // Views
```

### Target Variable
```java
relevance: Integer (0/1)  // 0 = No interaction, 1 = User interacted (upvote/comment/share)
```

---

## Implementation Roadmap

### Phase 1: Core Feature Extraction (Week 1-2)

**Goal**: Implement the top 5 most important features from research

#### Task 1.1: Enhance Content Analysis
```java
// New service: ContentAnalysisService.java
public class ContentAnalysisService {
    // Extract keywords from post content
    public List<String> extractKeywords(String content);
    
    // Extract hashtags from post content
    public List<String> extractHashtags(String content);
    
    // Check if content mentions user
    public boolean mentionsUser(String content, String username);
    
    // Detect URLs in content
    public boolean containsUrl(String content);
    
    // Calculate content length
    public int getContentLength(String content);
}
```

#### Task 1.2: Build User Interest Profile
```java
// New service: UserInterestProfileService.java
public class UserInterestProfileService {
    // Build user's keyword interest profile from history
    public Map<String, Double> buildKeywordProfile(Long userId);
    
    // Build user's hashtag interest profile
    public Map<String, Double> buildHashtagProfile(Long userId);
    
    // Calculate keyword relevance score
    public double calculateKeywordRelevance(
        List<String> postKeywords, 
        Map<String, Double> userProfile
    );
    
    // Calculate hashtag relevance score
    public double calculateHashtagRelevance(
        List<String> postHashtags,
        Map<String, Double> userProfile
    );
}
```

#### Task 1.3: Complete Author Features
```java
// Enhance: UserFeatures.java
@Builder
public class UserFeatures {
    private Long userId;
    private Integer followerCount;
    private Integer followingCount;
    private Double followersFollowingsRatio;  // NEW
    private Integer postCount;
    private Double engagementRate;
    private Integer accountAgeDays;            // NEW
    private Double accountAgeYears;            // NEW
}
```

#### Task 1.4: Fix Interaction Rate Calculation
```java
// Update: BehaviorFeaturesExtractionService.java
// Currently groups by postId, should group by authorId
private Map<Long, List<UserBehavior>> groupBehaviorsByAuthor(
    List<UserBehavior> behaviors
) {
    // Need to join with Post table to get authorId
    // Current implementation is incorrect
}
```

### Phase 2: Data Collection & Storage (Week 2-3)

#### Task 2.1: Create Training Data Table
```sql
CREATE TABLE feed_training_data (
    id BIGSERIAL PRIMARY KEY,
    user_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    author_id BIGINT NOT NULL,
    
    -- Content features
    keywords_relevance DOUBLE PRECISION,
    hashtags_relevance DOUBLE PRECISION,
    mentions_relevance INTEGER,
    content_length INTEGER,
    has_hashtags INTEGER,
    has_url INTEGER,
    has_multimedia INTEGER,
    
    -- Author features
    interaction_rate DOUBLE PRECISION,
    mention_count INTEGER,
    followers_followings_ratio DOUBLE PRECISION,
    author_seniority DOUBLE PRECISION,
    author_follower_count INTEGER,
    author_following_count INTEGER,
    author_post_count INTEGER,
    author_engagement_rate DOUBLE PRECISION,
    
    -- Relationship features
    follows INTEGER,
    interaction_count_7d INTEGER,
    interaction_count_30d INTEGER,
    hours_since_last_interaction DOUBLE PRECISION,
    affinity_score DOUBLE PRECISION,
    
    -- Engagement features
    popularity BIGINT,
    upvote_count BIGINT,
    downvote_count BIGINT,
    comment_count BIGINT,
    share_count BIGINT,
    view_count BIGINT,
    
    -- Target variable
    relevance INTEGER NOT NULL,  -- 0 or 1
    
    -- Metadata
    impression_time TIMESTAMP NOT NULL,
    interaction_time TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    
    INDEX idx_user_id (user_id),
    INDEX idx_post_id (post_id),
    INDEX idx_created_at (created_at)
);
```

#### Task 2.2: Implement Training Data Collection Service
```java
// New service: TrainingDataCollectionService.java
public class TrainingDataCollectionService {
    
    // Collect training sample when user views a post
    public void recordImpression(Long userId, Long postId, int position);
    
    // Update training sample when user interacts
    public void recordInteraction(Long userId, Long postId, EventType eventType);
    
    // Extract all features for a user-post pair
    public TrainingDataRecord extractTrainingFeatures(
        Long userId, 
        Long postId,
        LocalDateTime impressionTime
    );
    
    // Batch export training data for model training
    public List<TrainingDataRecord> exportTrainingData(
        LocalDateTime startDate,
        LocalDateTime endDate,
        int minInteractionsPerUser
    );
}
```

### Phase 3: Model Training Pipeline (Week 3-4)

#### Task 3.1: Setup Python ML Environment
```bash
# Create Python environment
cd ai/
python -m venv venv
source venv/bin/activate  # Windows: venv\Scripts\activate

# Install dependencies
pip install pandas numpy scikit-learn joblib flask
pip install xgboost  # For Gradient Boosting
```

#### Task 3.2: Create Training Script
```python
# ai/train_model.py
import pandas as pd
from sklearn.model_selection import train_test_split, RandomizedSearchCV
from sklearn.ensemble import GradientBoostingClassifier, RandomForestClassifier
from sklearn.preprocessing import MinMaxScaler
from sklearn.metrics import accuracy_score, f1_score, classification_report
import joblib

def load_training_data(csv_path):
    """Load training data from CSV export"""
    df = pd.read_csv(csv_path)
    return df

def preprocess_features(df):
    """Normalize features to [0, 1] range"""
    feature_columns = [
        'keywords_relevance', 'hashtags_relevance', 'mentions_relevance',
        'content_length', 'has_hashtags', 'has_url', 'has_multimedia',
        'interaction_rate', 'mention_count', 'followers_followings_ratio',
        'author_seniority', 'author_follower_count', 'author_following_count',
        'author_post_count', 'author_engagement_rate', 'follows',
        'interaction_count_7d', 'interaction_count_30d',
        'hours_since_last_interaction', 'affinity_score', 'popularity',
        'upvote_count', 'downvote_count', 'comment_count', 'share_count',
        'view_count'
    ]
    
    X = df[feature_columns]
    y = df['relevance']
    
    # Normalize features
    scaler = MinMaxScaler()
    X_scaled = scaler.fit_transform(X)
    
    return X_scaled, y, scaler, feature_columns

def train_gradient_boosting(X_train, y_train):
    """Train Gradient Boosting model with hyperparameter tuning"""
    param_dist = {
        'n_estimators': [50, 100, 150, 200],
        'learning_rate': [0.01, 0.05, 0.1, 0.2],
        'max_depth': [3, 5, 7, 9],
        'min_samples_split': [2, 5, 10],
        'min_samples_leaf': [1, 2, 4],
        'subsample': [0.8, 0.9, 1.0]
    }
    
    gb = GradientBoostingClassifier(random_state=42)
    
    search = RandomizedSearchCV(
        gb, param_dist, n_iter=150, cv=5,
        scoring='f1_weighted', random_state=42, n_jobs=-1
    )
    
    search.fit(X_train, y_train)
    return search.best_estimator_

def train_random_forest(X_train, y_train):
    """Train Random Forest model with hyperparameter tuning"""
    param_dist = {
        'n_estimators': [50, 100, 150, 200],
        'criterion': ['gini', 'entropy'],
        'max_depth': [3, 6, 9, 12, None],
        'min_samples_split': [2, 5, 10],
        'min_samples_leaf': [1, 2, 4],
        'max_features': ['sqrt', 'log2'],
        'bootstrap': [True, False]
    }
    
    rf = RandomForestClassifier(random_state=42)
    
    search = RandomizedSearchCV(
        rf, param_dist, n_iter=150, cv=5,
        scoring='f1_weighted', random_state=42, n_jobs=-1
    )
    
    search.fit(X_train, y_train)
    return search.best_estimator_

def evaluate_model(model, X_test, y_test):
    """Evaluate model performance"""
    y_pred = model.predict(X_test)
    
    accuracy = accuracy_score(y_test, y_pred)
    f1 = f1_score(y_test, y_pred, average='weighted')
    
    print(f"Accuracy: {accuracy:.4f}")
    print(f"F1-Score: {f1:.4f}")
    print("\nClassification Report:")
    print(classification_report(y_test, y_pred))
    
    return accuracy, f1

def main():
    # Load data
    df = load_training_data('data/training_data.csv')
    print(f"Loaded {len(df)} training samples")
    
    # Preprocess
    X, y, scaler, feature_columns = preprocess_features(df)
    
    # Split data (70-30, time-ordered)
    split_idx = int(len(X) * 0.7)
    X_train, X_test = X[:split_idx], X[split_idx:]
    y_train, y_test = y[:split_idx], y[split_idx:]
    
    print(f"Training set: {len(X_train)} samples")
    print(f"Test set: {len(X_test)} samples")
    
    # Train Gradient Boosting
    print("\n=== Training Gradient Boosting ===")
    gb_model = train_gradient_boosting(X_train, y_train)
    print("\nGradient Boosting Results:")
    evaluate_model(gb_model, X_test, y_test)
    
    # Train Random Forest
    print("\n=== Training Random Forest ===")
    rf_model = train_random_forest(X_train, y_train)
    print("\nRandom Forest Results:")
    evaluate_model(rf_model, X_test, y_test)
    
    # Save models
    joblib.dump(gb_model, 'models/gradient_boosting_model.pkl')
    joblib.dump(rf_model, 'models/random_forest_model.pkl')
    joblib.dump(scaler, 'models/scaler.pkl')
    joblib.dump(feature_columns, 'models/feature_columns.pkl')
    
    print("\nModels saved successfully!")

if __name__ == '__main__':
    main()
```

### Phase 4: Model Serving (Week 4-5)

#### Task 4.1: Create Flask Prediction API
```python
# ai/app/prediction_service.py
from flask import Flask, request, jsonify
import joblib
import numpy as np

app = Flask(__name__)

# Load models at startup
gb_model = joblib.load('models/gradient_boosting_model.pkl')
rf_model = joblib.load('models/random_forest_model.pkl')
scaler = joblib.load('models/scaler.pkl')
feature_columns = joblib.load('models/feature_columns.pkl')

@app.route('/predict', methods=['POST'])
def predict():
    """Predict relevance scores for a batch of posts"""
    data = request.json
    features = data['features']  # List of feature dicts
    
    # Convert to numpy array
    X = np.array([[f[col] for col in feature_columns] for f in features])
    
    # Scale features
    X_scaled = scaler.transform(X)
    
    # Predict with both models
    gb_proba = gb_model.predict_proba(X_scaled)[:, 1]
    rf_proba = rf_model.predict_proba(X_scaled)[:, 1]
    
    # Ensemble: average predictions
    ensemble_proba = (gb_proba + rf_proba) / 2
    
    return jsonify({
        'scores': ensemble_proba.tolist(),
        'model': 'ensemble_gb_rf'
    })

@app.route('/health', methods=['GET'])
def health():
    return jsonify({'status': 'healthy'})

if __name__ == '__main__':
    app.run(host='0.0.0.0', port=5000)
```

#### Task 4.2: Integrate with Java Backend
```java
// New service: AiRankingService.java
@Service
public class AiRankingService {
    private final RestTemplate restTemplate;
    private final String aiServiceUrl;
    
    public AiRankingService(
        RestTemplate restTemplate,
        @Value("${ai.service.url}") String aiServiceUrl
    ) {
        this.restTemplate = restTemplate;
        this.aiServiceUrl = aiServiceUrl;
    }
    
    public List<RankedPost> rankPosts(
        Long userId, 
        List<CandidatePost> candidates
    ) {
        // Extract features for all candidates
        List<Map<String, Object>> features = candidates.stream()
            .map(c -> extractAllFeatures(userId, c))
            .collect(Collectors.toList());
        
        // Call Python AI service
        AiRankingRequest request = new AiRankingRequest(features);
        AiRankingResponse response = restTemplate.postForObject(
            aiServiceUrl + "/predict",
            request,
            AiRankingResponse.class
        );
        
        // Combine scores with posts
        List<RankedPost> rankedPosts = new ArrayList<>();
        for (int i = 0; i < candidates.size(); i++) {
            rankedPosts.add(new RankedPost(
                candidates.get(i).getPost(),
                response.getScores().get(i)
            ));
        }
        
        // Sort by score descending
        rankedPosts.sort((a, b) -> 
            Double.compare(b.getScore(), a.getScore())
        );
        
        return rankedPosts;
    }
    
    private Map<String, Object> extractAllFeatures(
        Long userId, 
        CandidatePost candidate
    ) {
        // Extract all 26 features
        Map<String, Object> features = new HashMap<>();
        
        // Content features
        features.put("keywords_relevance", 
            calculateKeywordsRelevance(userId, candidate));
        features.put("hashtags_relevance", 
            calculateHashtagsRelevance(userId, candidate));
        // ... extract all 26 features
        
        return features;
    }
}
```

### Phase 5: A/B Testing & Monitoring (Week 5-6)

#### Task 5.1: Implement A/B Testing Framework
```java
// New service: FeedExperimentService.java
@Service
public class FeedExperimentService {
    
    public enum RankingStrategy {
        CHRONOLOGICAL,      // Baseline: time-ordered
        HOT_SCORE,          // Current: hot score ranking
        AI_RANKING,         // New: ML-based ranking
        HYBRID              // Combination
    }
    
    public RankingStrategy assignUserToExperiment(Long userId) {
        // Hash-based assignment for consistency
        int hash = userId.hashCode() % 100;
        
        if (hash < 20) return RankingStrategy.CHRONOLOGICAL;
        if (hash < 40) return RankingStrategy.HOT_SCORE;
        if (hash < 70) return RankingStrategy.AI_RANKING;
        return RankingStrategy.HYBRID;
    }
    
    public List<Post> rankFeed(
        Long userId, 
        List<CandidatePost> candidates,
        RankingStrategy strategy
    ) {
        switch (strategy) {
            case CHRONOLOGICAL:
                return rankByTime(candidates);
            case HOT_SCORE:
                return rankByHotScore(candidates);
            case AI_RANKING:
                return rankByAI(userId, candidates);
            case HYBRID:
                return rankByHybrid(userId, candidates);
            default:
                return rankByTime(candidates);
        }
    }
}
```

#### Task 5.2: Add Metrics Collection
```java
// New service: FeedMetricsService.java
@Service
public class FeedMetricsService {
    
    public void recordFeedImpression(
        Long userId,
        List<Long> postIds,
        RankingStrategy strategy
    ) {
        // Log to metrics system (Prometheus, CloudWatch, etc.)
    }
    
    public void recordEngagement(
        Long userId,
        Long postId,
        EventType eventType,
        int position,
        RankingStrategy strategy
    ) {
        // Track engagement metrics by strategy
    }
    
    public ExperimentMetrics calculateMetrics(
        RankingStrategy strategy,
        LocalDateTime startDate,
        LocalDateTime endDate
    ) {
        // Calculate:
        // - CTR (Click-Through Rate)
        // - Engagement Rate
        // - Avg Dwell Time
        // - Session Duration
        // - User Retention
        return new ExperimentMetrics();
    }
}
```

---

## Data Requirements

### Minimum Viable Dataset (MVP)
- **Users**: 50-100 active users
- **Training samples per user**: 150-200 interactions
- **Total training samples**: 7,500-20,000
- **Collection period**: 2-4 weeks
- **Expected accuracy**: ~75%

### Production Dataset (Optimal)
- **Users**: 500+ active users
- **Training samples per user**: 400-600 interactions
- **Total training samples**: 200,000+
- **Collection period**: 2-3 months
- **Expected accuracy**: 82-84%

### Data Collection Strategy
1. **Week 1-2**: Deploy impression/interaction tracking
2. **Week 3-4**: Collect baseline data with current ranking
3. **Week 5-6**: Train initial model on collected data
4. **Week 7+**: Deploy AI ranking to 30% of users, continue collecting

---

## Key Implementation Decisions

### 1. Feature Engineering Priorities

**Must Have (Top 5 features - 70% of model importance)**:
1. ✅ Interaction_rate (25-30%)
2. ❌ Keywords_relevance (18-22%)
3. ✅ Popularity (12-15%)
4. ❌ Hashtags_relevance (10-12%)
5. ⚠️ Followers_Followings (8-10%)

**Should Have (Next 5 features - 20% of model importance)**:
6. ⚠️ Listed_count or Author credibility (6-8%)
7. ✅ Length (4-6%)
8. ⚠️ Seniority (3-5%)
9. ⚠️ URL (2-4%)
10. ✅ Multimedia (2-3%)

**Nice to Have (Remaining features - 10% of model importance)**:
11-13. Mentions, Hashtags binary, Mention_count

### 2. Model Selection

**Recommended**: Start with **Random Forest**
- Pros: 81-83% accuracy, interpretable, feature importance analysis
- Cons: Slightly lower accuracy than GB

**Alternative**: **Gradient Boosting**
- Pros: 82-84% accuracy (best performance)
- Cons: Longer training time, less interpretable

**Production**: **Ensemble** (average of both)
- Pros: Best of both worlds, more robust
- Cons: 2x inference time

### 3. Cold Start Strategy

For new users with <150 interactions:
1. Use **content-based features** only (keywords, hashtags, popularity)
2. Use **global popularity** as fallback
3. Use **collaborative filtering** (similar users' preferences)
4. Gradually transition to personalized ranking as data accumulates

### 4. Real-Time vs Batch Ranking

**Hybrid Approach**:
- **Batch**: Pre-compute user interest profiles (hourly)
- **Real-Time**: Extract post features and predict on-demand
- **Cache**: Cache predictions for 5-10 minutes

---

## Success Metrics

### Model Performance Metrics
- **Accuracy**: Target 80%+ (research achieved 82-84%)
- **F1-Score**: Target 80%+ (handles class imbalance)
- **AUC-ROC**: Target 0.85+
- **Feature Importance**: Validate top features match research

### Business Metrics
- **Engagement Rate**: +15-25% increase
- **Session Duration**: +20-30% increase
- **User Retention**: +10-15% increase
- **Posts per Session**: +25-35% increase

### Operational Metrics
- **Prediction Latency**: <100ms p99
- **Model Retraining**: Weekly or bi-weekly
- **Data Freshness**: User profiles updated hourly

---

## Next Steps

### Immediate Actions (This Week)
1. ✅ Review this implementation guide
2. ❌ Implement `ContentAnalysisService` for keyword/hashtag extraction
3. ❌ Fix `BehaviorFeaturesExtractionService` to group by authorId
4. ❌ Add User table fields (createdAt for seniority)
5. ❌ Create `feed_training_data` table

### Short-Term (Next 2 Weeks)
1. Deploy impression/interaction tracking
2. Start collecting training data
3. Implement feature extraction for top 5 features
4. Build user interest profile service

### Medium-Term (Next 4-6 Weeks)
1. Collect 10,000+ training samples
2. Train initial models (GB + RF)
3. Deploy Python prediction service
4. Integrate with Java backend
5. Run A/B test with 30% of users

### Long-Term (2-3 Months)
1. Collect 100,000+ training samples
2. Retrain models with full feature set
3. Expand A/B test to 50% of users
4. Implement online learning for continuous improvement
5. Add advanced features (temporal patterns, network effects)

---

## Conclusion

Your Social Pulse project has a solid foundation for implementing AI-powered feed ranking. The research paper provides a proven blueprint with 82-84% accuracy. Your main gaps are:

1. **Content analysis** (keywords, hashtags, mentions)
2. **User interest profiling** (historical preferences)
3. **Training data collection** (impression + interaction tracking)
4. **Model training pipeline** (Python ML scripts)
5. **Prediction service** (Flask API + Java integration)

With focused effort over 4-6 weeks, you can deploy an MVP AI ranking system and start seeing engagement improvements. The key is to start with the top 5 features (70% of model importance) and iterate from there.

**Estimated Timeline**: 6-8 weeks to production-ready AI ranking
**Expected Impact**: 15-25% engagement increase, 20-30% session duration increase
**Risk Level**: Low (research-validated approach, fallback to current ranking)

Good luck with the implementation! 🚀
