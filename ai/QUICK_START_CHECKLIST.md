# AI Feed Ranking - Quick Start Checklist

**Project**: Social Pulse AI Feed Ranking  
**Goal**: Implement ML-based feed ranking achieving 82-84% accuracy  
**Timeline**: 6-8 weeks  

---

## 📋 Pre-Implementation Checklist

### Prerequisites
- [ ] Read EXECUTIVE_SUMMARY.md (understand the full scope)
- [ ] Read IMPLEMENTATION_GUIDE.md (understand the technical approach)
- [ ] Read FEATURE_EXTRACTION_PLAN.md (understand feature extraction)
- [ ] Review your current codebase structure
- [ ] Ensure you have database admin access
- [ ] Ensure you have Python 3.8+ installed
- [ ] Ensure you have access to deploy Flask services

---

## 🚀 Week 1: Foundation Setup

### Day 1: Database Setup
- [ ] Create feature branch: `git checkout -b feature/ai-feed-ranking`
- [ ] Review migration file: `ai/database/V1__create_training_data_table.sql`
- [ ] Backup your database: `pg_dump social_pulse > backup.sql`
- [ ] Run migration: `psql -U postgres -d social_pulse -f ai/database/V1__create_training_data_table.sql`
- [ ] Verify tables created:
  ```sql
  \dt feed_*
  -- Should see: feed_training_data, feed_impressions
  \dt user_interest_profiles
  ```
- [ ] Test helper function:
  ```sql
  SELECT calculate_interaction_rate(1, 2, 30);
  ```

### Day 2: Content Analysis Service
- [ ] Create file: `backend/src/main/java/com/socialpulse/app/feed/application/service/ContentAnalysisService.java`
- [ ] Copy code from `FEATURE_EXTRACTION_PLAN.md` (lines 20-180)
- [ ] Add Spring `@Service` annotation
- [ ] Create test file: `ContentAnalysisServiceTest.java`
- [ ] Test hashtag extraction:
  ```java
  @Test
  public void testExtractHashtags() {
      String content = "Check out #AI and #MachineLearning!";
      List<String> hashtags = service.extractHashtags(content);
      assertEquals(2, hashtags.size());
      assertTrue(hashtags.contains("ai"));
      assertTrue(hashtags.contains("machinelearning"));
  }
  ```
- [ ] Test keyword extraction
- [ ] Test URL detection
- [ ] Test mention extraction

### Day 3: User Interest Profile Service
- [ ] Create file: `backend/src/main/java/com/socialpulse/app/feed/application/service/UserInterestProfileService.java`
- [ ] Copy code from `FEATURE_EXTRACTION_PLAN.md` (lines 200-400)
- [ ] Add dependencies injection (UserBehaviorRepository, PostRepository, ContentAnalysisService)
- [ ] Create test file: `UserInterestProfileServiceTest.java`
- [ ] Test keyword profile building with mock data
- [ ] Test hashtag profile building with mock data
- [ ] Test relevance score calculation

### Day 4: New DTO Classes
- [ ] Create `backend/src/main/java/com/socialpulse/app/feed/application/dto/ContentFeatures.java`
- [ ] Create `backend/src/main/java/com/socialpulse/app/feed/application/dto/AuthorFeatures.java`
- [ ] Create `backend/src/main/java/com/socialpulse/app/feed/application/dto/RelationshipFeatures.java`
- [ ] Create `backend/src/main/java/com/socialpulse/app/feed/application/dto/EngagementFeatures.java`
- [ ] Create `backend/src/main/java/com/socialpulse/app/feed/application/dto/CompleteRankingFeatures.java`
- [ ] Copy code from `FEATURE_EXTRACTION_PLAN.md` (lines 450-550)
- [ ] Add Lombok annotations (@Getter, @Builder, @NoArgsConstructor, @AllArgsConstructor)
- [ ] Compile and verify no errors

### Day 5: Training Data Collection Service
- [ ] File already created: `backend/src/main/java/com/socialpulse/app/feed/application/service/TrainingDataCollectionService.java`
- [ ] Create domain models:
  - [ ] `FeedImpression.java` in `backend/.../feed/domain/model/`
  - [ ] `TrainingDataRecord.java` in `backend/.../feed/domain/model/`
  - [ ] `TrainingDataStats.java` in `backend/.../feed/domain/model/`
- [ ] Create repositories:
  - [ ] `FeedImpressionRepository.java` interface
  - [ ] `TrainingDataRepository.java` interface
  - [ ] JPA implementations
- [ ] Add `@Async` configuration in Spring Boot
- [ ] Test with mock data

---

## 🚀 Week 2: Integration & Data Collection

### Day 6: Integrate Impression Tracking
- [ ] Open `backend/.../feed/adapter/web/FeedController.java`
- [ ] Find the `getFeed()` method
- [ ] Inject `TrainingDataCollectionService`
- [ ] Add impression tracking after feed generation:
  ```java
  @GetMapping("/feed")
  public ResponseEntity<List<FeedItemResponse>> getFeed(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
      Long userId = extractUserId(userDetails);
      List<FeedItem> feed = getFeedService.getFeed(userId);
      
      // Track impressions for training data
      for (int i = 0; i < feed.size(); i++) {
          trainingDataCollectionService.recordImpression(
              userId, 
              feed.get(i).getPostId(), 
              i,  // position
              "HOT_SCORE"  // current strategy
          );
      }
      
      return ResponseEntity.ok(mapToResponse(feed));
  }
  ```
- [ ] Test in Postman/browser
- [ ] Verify impressions are recorded: `SELECT COUNT(*) FROM feed_impressions;`

### Day 7: Integrate Interaction Tracking
- [ ] Open `backend/.../post/adapter/web/PostController.java`
- [ ] Find upvote/downvote/comment/share endpoints
- [ ] Inject `TrainingDataCollectionService`
- [ ] Add interaction tracking:
  ```java
  @PostMapping("/{postId}/upvote")
  public ResponseEntity<?> upvotePost(
      @PathVariable Long postId,
      @AuthenticationPrincipal UserDetails userDetails
  ) {
      Long userId = extractUserId(userDetails);
      
      // Existing upvote logic
      reactPostService.upvote(userId, postId);
      
      // Track interaction for training data
      trainingDataCollectionService.recordInteraction(
          userId, 
          postId, 
          EventType.UPVOTE
      );
      
      return ResponseEntity.ok().build();
  }
  ```
- [ ] Add tracking to comment endpoint
- [ ] Add tracking to share endpoint
- [ ] Test all endpoints
- [ ] Verify interactions are recorded: `SELECT * FROM feed_training_data LIMIT 10;`

### Day 8: Scheduled Jobs
- [ ] Create `backend/.../feed/infrastructure/scheduler/TrainingDataScheduler.java`
- [ ] Add scheduled job for negative sample generation:
  ```java
  @Component
  public class TrainingDataScheduler {
      
      @Autowired
      private TrainingDataCollectionService trainingDataService;
      
      // Run every day at 2 AM
      @Scheduled(cron = "0 0 2 * * *")
      public void generateNegativeSamples() {
          log.info("Generating negative training samples...");
          trainingDataService.generateNegativeSamples(24);
          log.info("Negative samples generation completed");
      }
      
      // Refresh user profiles every hour
      @Scheduled(cron = "0 0 * * * *")
      public void refreshUserProfiles() {
          log.info("Refreshing user interest profiles...");
          // TODO: Get list of active users and refresh their profiles
          log.info("User profiles refresh completed");
      }
  }
  ```
- [ ] Enable scheduling in Spring Boot: `@EnableScheduling`
- [ ] Test scheduled jobs

### Day 9-10: Bug Fixes & Testing
- [ ] Fix `BehaviorFeaturesExtractionService.java` line 141:
  ```java
  // BEFORE (WRONG):
  private Map<Long, List<UserBehavior>> groupBehaviorsByAuthor(List<UserBehavior> behaviors) {
      return behaviors.stream()
          .collect(Collectors.groupingBy(UserBehavior::getPostId));  // BUG!
  }
  
  // AFTER (CORRECT):
  private Map<Long, List<UserBehavior>> groupBehaviorsByAuthor(List<UserBehavior> behaviors) {
      // Need to join with Post table to get authorId
      Set<Long> postIds = behaviors.stream()
          .map(UserBehavior::getPostId)
          .collect(Collectors.toSet());
      
      Map<Long, Long> postToAuthor = postRepository.findAllById(postIds)
          .stream()
          .collect(Collectors.toMap(Post::getId, Post::getUserId));
      
      return behaviors.stream()
          .filter(b -> postToAuthor.containsKey(b.getPostId()))
          .collect(Collectors.groupingBy(b -> postToAuthor.get(b.getPostId())));
  }
  ```
- [ ] Add User.createdAt field if missing
- [ ] Run all unit tests: `mvn test`
- [ ] Run integration tests
- [ ] Deploy to staging environment
- [ ] Monitor logs for errors

---

## 🚀 Week 3-4: Data Collection Period

### Daily Monitoring
- [ ] Check training data collection rate:
  ```sql
  SELECT * FROM training_data_summary ORDER BY date DESC LIMIT 7;
  ```
- [ ] Target: 500-1000 samples per day
- [ ] Check positive/negative ratio:
  ```sql
  SELECT 
      relevance,
      COUNT(*) as count,
      ROUND(COUNT(*) * 100.0 / SUM(COUNT(*)) OVER (), 2) as percentage
  FROM feed_training_data
  GROUP BY relevance;
  ```
- [ ] Target: 30-40% positive samples
- [ ] Check for errors in logs
- [ ] Fix any bugs in tracking

### Week 3 End: Data Quality Check
- [ ] Total samples collected: _______ (target: 7,000+)
- [ ] Unique users: _______ (target: 50+)
- [ ] Positive rate: _______ (target: 30-40%)
- [ ] Average samples per user: _______ (target: 100+)
- [ ] If targets not met, continue collecting for another week

---

## 🚀 Week 4: Model Training

### Day 1: Setup Python Environment
- [ ] Navigate to ai directory: `cd ai/`
- [ ] Create virtual environment: `python -m venv venv`
- [ ] Activate environment:
  - Windows: `venv\Scripts\activate`
  - Mac/Linux: `source venv/bin/activate`
- [ ] Create requirements.txt:
  ```
  pandas==2.0.3
  numpy==1.24.3
  scikit-learn==1.3.0
  xgboost==1.7.6
  joblib==1.3.1
  flask==2.3.2
  flask-cors==4.0.0
  ```
- [ ] Install dependencies: `pip install -r requirements.txt`
- [ ] Verify installation: `python -c "import sklearn; print(sklearn.__version__)"`

### Day 2: Export Training Data
- [ ] Export from database:
  ```sql
  COPY (
      SELECT 
          keywords_relevance, hashtags_relevance, mentions_relevance,
          content_length, has_hashtags, has_url, has_multimedia,
          interaction_rate, mention_count, followers_followings_ratio,
          author_seniority, author_follower_count, author_following_count,
          author_post_count, author_engagement_rate, follows,
          interaction_count_7d, interaction_count_30d,
          hours_since_last_interaction, affinity_score, popularity,
          upvote_count, downvote_count, comment_count, share_count,
          view_count, relevance
      FROM feed_training_data
      WHERE created_at >= NOW() - INTERVAL '30 days'
  ) TO '/tmp/training_data.csv' CSV HEADER;
  ```
- [ ] Copy to ai directory: `cp /tmp/training_data.csv ai/data/training_data.csv`
- [ ] Verify file: `wc -l ai/data/training_data.csv` (should be 7000+ lines)
- [ ] Check for nulls: `grep -c "NULL" ai/data/training_data.csv` (should be 0)

### Day 3: Train Models
- [ ] Create `ai/train_model.py` (copy from IMPLEMENTATION_GUIDE.md lines 200-350)
- [ ] Create directories:
  ```bash
  mkdir -p ai/models
  mkdir -p ai/logs
  ```
- [ ] Run training script:
  ```bash
  python train_model.py 2>&1 | tee logs/training_$(date +%Y%m%d).log
  ```
- [ ] Expected output:
  ```
  Loaded 10000 training samples
  Training set: 7000 samples
  Test set: 3000 samples
  
  === Training Gradient Boosting ===
  Best parameters: {...}
  Gradient Boosting Results:
  Accuracy: 0.7823
  F1-Score: 0.7756
  
  === Training Random Forest ===
  Best parameters: {...}
  Random Forest Results:
  Accuracy: 0.7691
  F1-Score: 0.7634
  
  Models saved successfully!
  ```
- [ ] Verify models saved:
  ```bash
  ls -lh ai/models/
  # Should see:
  # gradient_boosting_model.pkl
  # random_forest_model.pkl
  # scaler.pkl
  # feature_columns.pkl
  ```
- [ ] Check model accuracy: _______ (target: 75%+)

### Day 4: Create Prediction Service
- [ ] Create `ai/app/prediction_service.py` (copy from IMPLEMENTATION_GUIDE.md lines 400-450)
- [ ] Test locally:
  ```bash
  cd ai/app
  python prediction_service.py
  ```
- [ ] In another terminal, test endpoint:
  ```bash
  curl -X POST http://localhost:5000/predict \
    -H "Content-Type: application/json" \
    -d '{
      "features": [{
        "keywords_relevance": 100.0,
        "hashtags_relevance": 50.0,
        "mentions_relevance": 0,
        "content_length": 120,
        "has_hashtags": 1,
        "has_url": 1,
        "has_multimedia": 0,
        "interaction_rate": 0.5,
        "mention_count": 0,
        "followers_followings_ratio": 100.0,
        "author_seniority": 2.0,
        "author_follower_count": 500,
        "author_following_count": 200,
        "author_post_count": 100,
        "author_engagement_rate": 0.3,
        "follows": 1,
        "interaction_count_7d": 5,
        "interaction_count_30d": 20,
        "hours_since_last_interaction": 12.0,
        "affinity_score": 0.8,
        "popularity": 100,
        "upvote_count": 50,
        "downvote_count": 5,
        "comment_count": 20,
        "share_count": 10,
        "view_count": 500
      }]
    }'
  ```
- [ ] Expected response:
  ```json
  {
    "scores": [0.8234],
    "model": "ensemble_gb_rf"
  }
  ```
- [ ] Test health endpoint: `curl http://localhost:5000/health`

### Day 5: Deploy Prediction Service
- [ ] Create Dockerfile:
  ```dockerfile
  FROM python:3.9-slim
  WORKDIR /app
  COPY requirements.txt .
  RUN pip install --no-cache-dir -r requirements.txt
  COPY app/ ./app/
  COPY models/ ./models/
  EXPOSE 5000
  CMD ["python", "app/prediction_service.py"]
  ```
- [ ] Build Docker image:
  ```bash
  docker build -t social-pulse-ai:latest .
  ```
- [ ] Run container:
  ```bash
  docker run -d -p 5000:5000 --name social-pulse-ai social-pulse-ai:latest
  ```
- [ ] Test container: `curl http://localhost:5000/health`
- [ ] Deploy to production server
- [ ] Update application.properties:
  ```properties
  ai.service.url=http://localhost:5000
  ```

---

## 🚀 Week 5: Backend Integration

### Day 1: AI Ranking Service
- [ ] Create `backend/.../feed/application/service/AiRankingService.java`
- [ ] Copy code from IMPLEMENTATION_GUIDE.md (lines 500-600)
- [ ] Add RestTemplate configuration
- [ ] Implement feature extraction for all 26 features
- [ ] Add error handling and fallback logic
- [ ] Write unit tests with mocked AI service

### Day 2: Update Feed Service
- [ ] Open `backend/.../feed/application/service/GetFeedService.java`
- [ ] Inject `AiRankingService`
- [ ] Add AI ranking option:
  ```java
  public List<FeedItem> getFeed(Long userId, String rankingStrategy) {
      // Get candidate posts
      List<CandidatePost> candidates = candidateSelectionService.selectCandidates(userId);
      
      // Rank based on strategy
      List<Post> rankedPosts;
      if ("AI_RANKING".equals(rankingStrategy)) {
          try {
              rankedPosts = aiRankingService.rankPosts(userId, candidates);
          } catch (Exception e) {
              log.error("AI ranking failed, falling back to hot score", e);
              rankedPosts = rankByHotScore(candidates);
          }
      } else {
          rankedPosts = rankByHotScore(candidates);
      }
      
      return mapToFeedItems(rankedPosts);
  }
  ```
- [ ] Test with AI ranking enabled
- [ ] Test fallback when AI service is down

### Day 3: A/B Testing Framework
- [ ] Create `backend/.../feed/application/service/FeedExperimentService.java`
- [ ] Copy code from IMPLEMENTATION_GUIDE.md (lines 650-700)
- [ ] Implement user assignment logic (hash-based)
- [ ] Update FeedController to use experiment service:
  ```java
  @GetMapping("/feed")
  public ResponseEntity<List<FeedItemResponse>> getFeed(
      @AuthenticationPrincipal UserDetails userDetails
  ) {
      Long userId = extractUserId(userDetails);
      
      // Assign user to experiment group
      RankingStrategy strategy = experimentService.assignUserToExperiment(userId);
      
      // Get feed with assigned strategy
      List<FeedItem> feed = getFeedService.getFeed(userId, strategy.name());
      
      // Track impressions with strategy
      trackImpressions(userId, feed, strategy.name());
      
      return ResponseEntity.ok(mapToResponse(feed));
  }
  ```
- [ ] Test user assignment consistency (same user always gets same group)

### Day 4: Metrics Collection
- [ ] Create `backend/.../feed/application/service/FeedMetricsService.java`
- [ ] Implement metrics tracking:
  - Engagement rate by strategy
  - Session duration by strategy
  - Posts per session by strategy
- [ ] Add metrics endpoints:
  ```java
  @GetMapping("/admin/metrics/experiment")
  public ResponseEntity<ExperimentMetrics> getExperimentMetrics(
      @RequestParam String strategy,
      @RequestParam LocalDate startDate,
      @RequestParam LocalDate endDate
  ) {
      ExperimentMetrics metrics = metricsService.calculateMetrics(
          RankingStrategy.valueOf(strategy),
          startDate.atStartOfDay(),
          endDate.atTime(23, 59, 59)
      );
      return ResponseEntity.ok(metrics);
  }
  ```
- [ ] Create dashboard queries:
  ```sql
  -- Engagement rate by strategy
  SELECT 
      fi.ranking_strategy,
      COUNT(DISTINCT fi.user_id) as users,
      COUNT(*) as impressions,
      SUM(CASE WHEN fi.interacted THEN 1 ELSE 0 END) as interactions,
      ROUND(SUM(CASE WHEN fi.interacted THEN 1 ELSE 0 END)::numeric / COUNT(*) * 100, 2) as engagement_rate
  FROM feed_impressions fi
  WHERE fi.impression_time >= NOW() - INTERVAL '7 days'
  GROUP BY fi.ranking_strategy;
  ```

### Day 5: Deploy to Production
- [ ] Merge feature branch: `git merge feature/ai-feed-ranking`
- [ ] Tag release: `git tag -a v1.0.0-ai-ranking -m "AI-powered feed ranking"`
- [ ] Deploy backend to production
- [ ] Deploy AI service to production
- [ ] Verify health checks pass
- [ ] Monitor logs for errors
- [ ] Start A/B test with 30% of users

---

## 🚀 Week 6: Monitoring & Optimization

### Daily Monitoring
- [ ] Check AI service uptime: `curl http://ai-service:5000/health`
- [ ] Check prediction latency (target: <100ms p99)
- [ ] Check engagement metrics by strategy
- [ ] Check error rates in logs
- [ ] Fix any bugs discovered

### Week 6 End: A/B Test Results
- [ ] Calculate engagement rate improvement: _______ (target: +15%)
- [ ] Calculate session duration improvement: _______ (target: +20%)
- [ ] Calculate retention improvement: _______ (target: +10%)
- [ ] Statistical significance: _______ (target: p < 0.05)
- [ ] Decision: [ ] Roll out to 100% [ ] Continue testing [ ] Rollback

---

## 📊 Success Criteria

### MVP Success (Week 4)
- [ ] Model accuracy: 75%+ ✅
- [ ] Prediction latency: <200ms p99 ✅
- [ ] Training data: 10,000+ samples ✅
- [ ] No critical bugs ✅

### Production Success (Week 6)
- [ ] Model accuracy: 80%+ ✅
- [ ] Prediction latency: <100ms p99 ✅
- [ ] Engagement rate: +15% vs baseline ✅
- [ ] Session duration: +20% vs baseline ✅
- [ ] User retention: +10% vs baseline ✅
- [ ] AI service uptime: 99.9%+ ✅

---

## 🆘 Troubleshooting

### Issue: No training data being collected
- [ ] Check if impressions are being recorded: `SELECT COUNT(*) FROM feed_impressions;`
- [ ] Check if interactions are being recorded: `SELECT COUNT(*) FROM feed_training_data WHERE relevance = 1;`
- [ ] Check logs for errors in TrainingDataCollectionService
- [ ] Verify @Async is enabled in Spring Boot

### Issue: Low positive rate (<20%)
- [ ] Check if interaction tracking is working
- [ ] Verify EventType mapping is correct
- [ ] Consider adding more positive event types (e.g., CLICK, DWELL)
- [ ] Check if users are actually engaging with content

### Issue: Model accuracy <70%
- [ ] Check for feature extraction bugs (especially keywords_relevance)
- [ ] Verify training data quality (no nulls, correct ranges)
- [ ] Check class imbalance (should be 30-40% positive)
- [ ] Collect more training data (need 20K+ samples)

### Issue: High prediction latency (>200ms)
- [ ] Check AI service response time
- [ ] Add caching for user interest profiles
- [ ] Batch predictions instead of one-by-one
- [ ] Optimize feature extraction queries

### Issue: AI service keeps crashing
- [ ] Check memory usage (models are large)
- [ ] Check for null values in features
- [ ] Add input validation in Flask API
- [ ] Check Docker container logs

---

## 📝 Notes

- Keep this checklist updated as you progress
- Mark items complete with [x] instead of [ ]
- Add notes for any deviations from the plan
- Document any bugs or issues encountered
- Share progress with team weekly

---

**Last Updated**: 2026-04-30  
**Status**: Ready to Start  
**Next Action**: Day 1 - Database Setup
