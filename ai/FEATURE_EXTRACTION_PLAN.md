# Feature Extraction Implementation Plan

## Overview

This document provides detailed implementation steps for extracting the 13 core features from the research paper, mapped to your Social Pulse domain.

---

## Phase 1: Content Analysis Service

### 1.1 ContentAnalysisService.java

**Location**: `backend/src/main/java/com/socialpulse/app/feed/application/service/ContentAnalysisService.java`

**Purpose**: Extract content-based features from post text

```java
package com.socialpulse.app.feed.application.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
public class ContentAnalysisService {
    
    // Regex patterns
    private static final Pattern HASHTAG_PATTERN = Pattern.compile("#\\w+");
    private static final Pattern MENTION_PATTERN = Pattern.compile("@\\w+");
    private static final Pattern URL_PATTERN = Pattern.compile(
        "https?://[\\w\\-._~:/?#\\[\\]@!$&'()*+,;=]+"
    );
    
    // Common stop words to filter out
    private static final Set<String> STOP_WORDS = Set.of(
        "the", "a", "an", "and", "or", "but", "in", "on", "at", "to", "for",
        "of", "with", "by", "from", "as", "is", "was", "are", "were", "be",
        "been", "being", "have", "has", "had", "do", "does", "did", "will",
        "would", "should", "could", "may", "might", "must", "can", "this",
        "that", "these", "those", "i", "you", "he", "she", "it", "we", "they"
    );
    
    /**
     * Extract hashtags from post content
     * @param content Post content text
     * @return List of hashtags (without # symbol)
     */
    public List<String> extractHashtags(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> hashtags = new ArrayList<>();
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String hashtag = matcher.group().substring(1).toLowerCase();
            hashtags.add(hashtag);
        }
        
        return hashtags;
    }
    
    /**
     * Extract mentions from post content
     * @param content Post content text
     * @return List of mentioned usernames (without @ symbol)
     */
    public List<String> extractMentions(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        
        List<String> mentions = new ArrayList<>();
        Matcher matcher = MENTION_PATTERN.matcher(content);
        
        while (matcher.find()) {
            String mention = matcher.group().substring(1).toLowerCase();
            mentions.add(mention);
        }
        
        return mentions;
    }
    
    /**
     * Check if content mentions a specific user
     * @param content Post content text
     * @param username Username to check (without @)
     * @return true if user is mentioned
     */
    public boolean mentionsUser(String content, String username) {
        if (content == null || username == null) {
            return false;
        }
        
        List<String> mentions = extractMentions(content);
        return mentions.contains(username.toLowerCase());
    }
    
    /**
     * Check if content contains URLs
     * @param content Post content text
     * @return true if content contains at least one URL
     */
    public boolean containsUrl(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        Matcher matcher = URL_PATTERN.matcher(content);
        return matcher.find();
    }
    
    /**
     * Check if content contains hashtags
     * @param content Post content text
     * @return true if content contains at least one hashtag
     */
    public boolean containsHashtags(String content) {
        if (content == null || content.isEmpty()) {
            return false;
        }
        
        Matcher matcher = HASHTAG_PATTERN.matcher(content);
        return matcher.find();
    }
    
    /**
     * Get content length (character count)
     * @param content Post content text
     * @return Character count
     */
    public int getContentLength(String content) {
        return content == null ? 0 : content.length();
    }
    
    /**
     * Extract keywords from content (simple word tokenization)
     * @param content Post content text
     * @return List of keywords (lowercase, no stop words)
     */
    public List<String> extractKeywords(String content) {
        if (content == null || content.isEmpty()) {
            return Collections.emptyList();
        }
        
        // Remove URLs, hashtags, mentions
        String cleanContent = content
            .replaceAll(URL_PATTERN.pattern(), "")
            .replaceAll(HASHTAG_PATTERN.pattern(), "")
            .replaceAll(MENTION_PATTERN.pattern(), "");
        
        // Tokenize by word boundaries
        String[] words = cleanContent.toLowerCase()
            .replaceAll("[^a-z0-9\\s]", " ")
            .split("\\s+");
        
        // Filter stop words and short words
        return Arrays.stream(words)
            .filter(word -> word.length() > 2)
            .filter(word -> !STOP_WORDS.contains(word))
            .collect(Collectors.toList());
    }
    
    /**
     * Calculate keyword frequency map
     * @param keywords List of keywords
     * @return Map of keyword to frequency
     */
    public Map<String, Integer> calculateKeywordFrequency(List<String> keywords) {
        Map<String, Integer> frequency = new HashMap<>();
        
        for (String keyword : keywords) {
            frequency.put(keyword, frequency.getOrDefault(keyword, 0) + 1);
        }
        
        return frequency;
    }
    
    /**
     * Calculate TF-IDF style keyword weights
     * @param keywords Keywords from current post
     * @param totalPosts Total number of posts in corpus
     * @param keywordDocumentFrequency Map of keyword to number of documents containing it
     * @return Map of keyword to TF-IDF weight
     */
    public Map<String, Double> calculateKeywordWeights(
        List<String> keywords,
        int totalPosts,
        Map<String, Integer> keywordDocumentFrequency
    ) {
        Map<String, Integer> termFrequency = calculateKeywordFrequency(keywords);
        Map<String, Double> weights = new HashMap<>();
        
        for (Map.Entry<String, Integer> entry : termFrequency.entrySet()) {
            String keyword = entry.getKey();
            int tf = entry.getValue();
            int df = keywordDocumentFrequency.getOrDefault(keyword, 1);
            
            // TF-IDF = TF * log(N / DF)
            double tfidf = tf * Math.log((double) totalPosts / df);
            weights.put(keyword, tfidf);
        }
        
        return weights;
    }
}
```

---

## Phase 2: User Interest Profile Service

### 2.1 UserInterestProfileService.java

**Location**: `backend/src/main/java/com/socialpulse/app/feed/application/service/UserInterestProfileService.java`

**Purpose**: Build and maintain user interest profiles based on historical interactions

```java
package com.socialpulse.app.feed.application.service;

import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.domain.model.UserBehavior;
import com.socialpulse.app.behavior.domain.repository.UserBehaviorRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.post.domain.repository.PostRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class UserInterestProfileService {
    
    private final UserBehaviorRepository behaviorRepository;
    private final PostRepository postRepository;
    private final ContentAnalysisService contentAnalysisService;
    private final StringRedisTemplate redisTemplate;
    
    // Event type weights for interest calculation
    private static final Map<EventType, Double> EVENT_WEIGHTS = Map.of(
        EventType.CLICK, 1.0,
        EventType.UPVOTE, 3.0,
        EventType.COMMENT, 5.0,
        EventType.SHARE, 7.0,
        EventType.FOLLOW, 10.0,
        EventType.DOWNVOTE, -2.0,
        EventType.HIDE, -5.0,
        EventType.REPORT, -10.0
    );
    
    public UserInterestProfileService(
        UserBehaviorRepository behaviorRepository,
        PostRepository postRepository,
        ContentAnalysisService contentAnalysisService,
        StringRedisTemplate redisTemplate
    ) {
        this.behaviorRepository = behaviorRepository;
        this.postRepository = postRepository;
        this.contentAnalysisService = contentAnalysisService;
        this.redisTemplate = redisTemplate;
    }
    
    /**
     * Build keyword interest profile for a user
     * @param userId User ID
     * @return Map of keyword to interest score
     */
    public Map<String, Double> buildKeywordProfile(Long userId) {
        // Check cache first
        String cacheKey = "user:keyword_profile:" + userId;
        // TODO: Implement cache retrieval
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<UserBehavior> behaviors = behaviorRepository.findByUserIdSince(userId, thirtyDaysAgo);
        
        // Get posts that user interacted with
        Set<Long> postIds = behaviors.stream()
            .map(UserBehavior::getPostId)
            .collect(Collectors.toSet());
        
        List<Post> posts = postRepository.findAllById(postIds);
        Map<Long, Post> postMap = posts.stream()
            .collect(Collectors.toMap(Post::getId, p -> p));
        
        // Calculate weighted keyword scores
        Map<String, Double> keywordScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            Post post = postMap.get(behavior.getPostId());
            if (post == null || post.getContent() == null) continue;
            
            // Extract keywords from post
            List<String> keywords = contentAnalysisService.extractKeywords(post.getContent());
            
            // Get event weight
            double eventWeight = EVENT_WEIGHTS.getOrDefault(behavior.getEventType(), 0.0);
            
            // Apply time decay
            long hoursAgo = Duration.between(behavior.getEventTime(), LocalDateTime.now()).toHours();
            double timeDecay = Math.exp(-hoursAgo / (30.0 * 24.0));
            
            double weight = eventWeight * timeDecay;
            
            // Update keyword scores
            for (String keyword : keywords) {
                keywordScores.put(keyword, 
                    keywordScores.getOrDefault(keyword, 0.0) + weight);
            }
        }
        
        // Normalize scores
        double maxScore = keywordScores.values().stream()
            .max(Double::compare)
            .orElse(1.0);
        
        if (maxScore > 0) {
            keywordScores.replaceAll((k, v) -> v / maxScore);
        }
        
        // Cache for 1 hour
        // TODO: Implement cache storage
        
        return keywordScores;
    }
    
    /**
     * Build hashtag interest profile for a user
     * @param userId User ID
     * @return Map of hashtag to interest score
     */
    public Map<String, Double> buildHashtagProfile(Long userId) {
        String cacheKey = "user:hashtag_profile:" + userId;
        
        LocalDateTime thirtyDaysAgo = LocalDateTime.now().minusDays(30);
        List<UserBehavior> behaviors = behaviorRepository.findByUserIdSince(userId, thirtyDaysAgo);
        
        Set<Long> postIds = behaviors.stream()
            .map(UserBehavior::getPostId)
            .collect(Collectors.toSet());
        
        List<Post> posts = postRepository.findAllById(postIds);
        Map<Long, Post> postMap = posts.stream()
            .collect(Collectors.toMap(Post::getId, p -> p));
        
        Map<String, Double> hashtagScores = new HashMap<>();
        
        for (UserBehavior behavior : behaviors) {
            Post post = postMap.get(behavior.getPostId());
            if (post == null || post.getContent() == null) continue;
            
            List<String> hashtags = contentAnalysisService.extractHashtags(post.getContent());
            double eventWeight = EVENT_WEIGHTS.getOrDefault(behavior.getEventType(), 0.0);
            
            long hoursAgo = Duration.between(behavior.getEventTime(), LocalDateTime.now()).toHours();
            double timeDecay = Math.exp(-hoursAgo / (30.0 * 24.0));
            double weight = eventWeight * timeDecay;
            
            for (String hashtag : hashtags) {
                hashtagScores.put(hashtag, 
                    hashtagScores.getOrDefault(hashtag, 0.0) + weight);
            }
        }
        
        // Normalize
        double maxScore = hashtagScores.values().stream()
            .max(Double::compare)
            .orElse(1.0);
        
        if (maxScore > 0) {
            hashtagScores.replaceAll((k, v) -> v / maxScore);
        }
        
        return hashtagScores;
    }
    
    /**
     * Calculate keyword relevance score for a post
     * @param postKeywords Keywords extracted from post
     * @param userProfile User's keyword interest profile
     * @return Relevance score (0-1000)
     */
    public double calculateKeywordRelevance(
        List<String> postKeywords,
        Map<String, Double> userProfile
    ) {
        if (postKeywords.isEmpty() || userProfile.isEmpty()) {
            return 0.0;
        }
        
        // Calculate cosine similarity
        double dotProduct = 0.0;
        double postMagnitude = 0.0;
        double profileMagnitude = 0.0;
        
        Map<String, Integer> postKeywordFreq = new HashMap<>();
        for (String keyword : postKeywords) {
            postKeywordFreq.put(keyword, postKeywordFreq.getOrDefault(keyword, 0) + 1);
        }
        
        for (Map.Entry<String, Integer> entry : postKeywordFreq.entrySet()) {
            String keyword = entry.getKey();
            double postWeight = entry.getValue();
            double profileWeight = userProfile.getOrDefault(keyword, 0.0);
            
            dotProduct += postWeight * profileWeight;
            postMagnitude += postWeight * postWeight;
        }
        
        for (Double weight : userProfile.values()) {
            profileMagnitude += weight * weight;
        }
        
        postMagnitude = Math.sqrt(postMagnitude);
        profileMagnitude = Math.sqrt(profileMagnitude);
        
        if (postMagnitude == 0 || profileMagnitude == 0) {
            return 0.0;
        }
        
        double cosineSimilarity = dotProduct / (postMagnitude * profileMagnitude);
        
        // Scale to 0-1000 range (matching research paper)
        return cosineSimilarity * 1000.0;
    }
    
    /**
     * Calculate hashtag relevance score for a post
     * @param postHashtags Hashtags extracted from post
     * @param userProfile User's hashtag interest profile
     * @return Relevance score (0-1000)
     */
    public double calculateHashtagRelevance(
        List<String> postHashtags,
        Map<String, Double> userProfile
    ) {
        if (postHashtags.isEmpty() || userProfile.isEmpty()) {
            return 0.0;
        }
        
        // Simple weighted overlap score
        double totalScore = 0.0;
        
        for (String hashtag : postHashtags) {
            totalScore += userProfile.getOrDefault(hashtag, 0.0);
        }
        
        // Normalize by number of hashtags and scale to 0-1000
        double avgScore = totalScore / postHashtags.size();
        return avgScore * 1000.0;
    }
    
    /**
     * Refresh user interest profiles (run periodically)
     * @param userId User ID
     */
    public void refreshUserProfile(Long userId) {
        log.info("Refreshing interest profile for user {}", userId);
        
        Map<String, Double> keywordProfile = buildKeywordProfile(userId);
        Map<String, Double> hashtagProfile = buildHashtagProfile(userId);
        
        // Cache profiles
        String keywordCacheKey = "user:keyword_profile:" + userId;
        String hashtagCacheKey = "user:hashtag_profile:" + userId;
        
        // TODO: Serialize and cache profiles
        
        log.info("Refreshed profile for user {}: {} keywords, {} hashtags",
            userId, keywordProfile.size(), hashtagProfile.size());
    }
}
```

---

## Phase 3: Enhanced Feature Extraction Service

### 3.1 Update FeatureExtractionService.java

Add methods to extract all 13 core features:

```java
// Add to existing FeatureExtractionService.java

/**
 * Extract complete feature set for AI ranking
 */
public CompleteRankingFeatures extractCompleteFeatures(
    Long viewerId, 
    CandidatePost candidate
) {
    Post post = candidate.getPost();
    Long authorId = post.getUserId();
    
    // Content features
    ContentFeatures contentFeatures = extractContentFeatures(viewerId, post);
    
    // Author features
    AuthorFeatures authorFeatures = extractAuthorFeatures(authorId);
    
    // Relationship features
    RelationshipFeatures relationshipFeatures = extractRelationshipFeatures(viewerId, authorId);
    
    // Engagement features
    EngagementFeatures engagementFeatures = extractEngagementFeatures(post);
    
    return CompleteRankingFeatures.builder()
        .postId(post.getId())
        .contentFeatures(contentFeatures)
        .authorFeatures(authorFeatures)
        .relationshipFeatures(relationshipFeatures)
        .engagementFeatures(engagementFeatures)
        .build();
}

private ContentFeatures extractContentFeatures(Long viewerId, Post post) {
    String content = post.getContent();
    
    // Extract keywords and hashtags
    List<String> keywords = contentAnalysisService.extractKeywords(content);
    List<String> hashtags = contentAnalysisService.extractHashtags(content);
    
    // Get user interest profiles
    Map<String, Double> keywordProfile = userInterestProfileService.buildKeywordProfile(viewerId);
    Map<String, Double> hashtagProfile = userInterestProfileService.buildHashtagProfile(viewerId);
    
    // Calculate relevance scores
    double keywordsRelevance = userInterestProfileService.calculateKeywordRelevance(
        keywords, keywordProfile
    );
    double hashtagsRelevance = userInterestProfileService.calculateHashtagRelevance(
        hashtags, hashtagProfile
    );
    
    // Check if viewer is mentioned
    // TODO: Get viewer username
    boolean mentionsRelevance = false; // contentAnalysisService.mentionsUser(content, viewerUsername);
    
    return ContentFeatures.builder()
        .keywordsRelevance(keywordsRelevance)
        .hashtagsRelevance(hashtagsRelevance)
        .mentionsRelevance(mentionsRelevance ? 1 : 0)
        .contentLength(contentAnalysisService.getContentLength(content))
        .hasHashtags(contentAnalysisService.containsHashtags(content) ? 1 : 0)
        .hasUrl(contentAnalysisService.containsUrl(content) ? 1 : 0)
        .hasMultimedia(post.getImageUrl() != null ? 1 : 0)
        .build();
}

private AuthorFeatures extractAuthorFeatures(Long authorId) {
    // TODO: Query user stats from database
    // For now, return placeholder
    return AuthorFeatures.builder()
        .authorId(authorId)
        .followerCount(0)
        .followingCount(0)
        .followersFollowingsRatio(0.0)
        .seniority(0.0)
        .postCount(0)
        .engagementRate(0.0)
        .build();
}

private RelationshipFeatures extractRelationshipFeatures(Long viewerId, Long authorId) {
    // Use existing BehaviorFeaturesExtractionService
    // TODO: Fix to properly group by authorId
    return RelationshipFeatures.builder()
        .follows(0)
        .interactionCount7d(0)
        .interactionCount30d(0)
        .hoursSinceLastInteraction(999.0)
        .affinityScore(0.0)
        .build();
}

private EngagementFeatures extractEngagementFeatures(Post post) {
    long popularity = safeCount(post.getUpvoteCount()) 
        + safeCount(post.getDownvoteCount())
        + safeCount(post.getCmtCount())
        + safeCount(post.getShareCount())
        + safeCount(post.getViewCount());
    
    return EngagementFeatures.builder()
        .popularity(popularity)
        .upvoteCount(safeCount(post.getUpvoteCount()))
        .downvoteCount(safeCount(post.getDownvoteCount()))
        .commentCount(safeCount(post.getCmtCount()))
        .shareCount(safeCount(post.getShareCount()))
        .viewCount(safeCount(post.getViewCount()))
        .build();
}

private long safeCount(Long value) {
    return value == null ? 0L : value;
}
```

---

## Phase 4: New DTOs for Complete Features

### 4.1 CompleteRankingFeatures.java

```java
package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CompleteRankingFeatures {
    private Long postId;
    private ContentFeatures contentFeatures;
    private AuthorFeatures authorFeatures;
    private RelationshipFeatures relationshipFeatures;
    private EngagementFeatures engagementFeatures;
    
    /**
     * Convert to flat map for AI service
     */
    public Map<String, Object> toFeatureMap() {
        Map<String, Object> features = new HashMap<>();
        
        // Content features
        features.put("keywords_relevance", contentFeatures.getKeywordsRelevance());
        features.put("hashtags_relevance", contentFeatures.getHashtagsRelevance());
        features.put("mentions_relevance", contentFeatures.getMentionsRelevance());
        features.put("content_length", contentFeatures.getContentLength());
        features.put("has_hashtags", contentFeatures.getHasHashtags());
        features.put("has_url", contentFeatures.getHasUrl());
        features.put("has_multimedia", contentFeatures.getHasMultimedia());
        
        // Author features
        features.put("follower_count", authorFeatures.getFollowerCount());
        features.put("following_count", authorFeatures.getFollowingCount());
        features.put("followers_followings_ratio", authorFeatures.getFollowersFollowingsRatio());
        features.put("author_seniority", authorFeatures.getSeniority());
        features.put("author_post_count", authorFeatures.getPostCount());
        features.put("author_engagement_rate", authorFeatures.getEngagementRate());
        
        // Relationship features
        features.put("follows", relationshipFeatures.getFollows());
        features.put("interaction_count_7d", relationshipFeatures.getInteractionCount7d());
        features.put("interaction_count_30d", relationshipFeatures.getInteractionCount30d());
        features.put("hours_since_last_interaction", relationshipFeatures.getHoursSinceLastInteraction());
        features.put("affinity_score", relationshipFeatures.getAffinityScore());
        
        // Engagement features
        features.put("popularity", engagementFeatures.getPopularity());
        features.put("upvote_count", engagementFeatures.getUpvoteCount());
        features.put("downvote_count", engagementFeatures.getDownvoteCount());
        features.put("comment_count", engagementFeatures.getCommentCount());
        features.put("share_count", engagementFeatures.getShareCount());
        features.put("view_count", engagementFeatures.getViewCount());
        
        return features;
    }
}
```

### 4.2 ContentFeatures.java

```java
package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ContentFeatures {
    private Double keywordsRelevance;      // 0-1000
    private Double hashtagsRelevance;      // 0-1000
    private Integer mentionsRelevance;     // 0 or 1
    private Integer contentLength;         // character count
    private Integer hasHashtags;           // 0 or 1
    private Integer hasUrl;                // 0 or 1
    private Integer hasMultimedia;         // 0 or 1
}
```

### 4.3 AuthorFeatures.java

```java
package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorFeatures {
    private Long authorId;
    private Integer followerCount;
    private Integer followingCount;
    private Double followersFollowingsRatio;
    private Double seniority;              // years
    private Integer postCount;
    private Double engagementRate;
}
```

### 4.4 RelationshipFeatures.java

```java
package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RelationshipFeatures {
    private Integer follows;                      // 0 or 1
    private Integer interactionCount7d;
    private Integer interactionCount30d;
    private Double hoursSinceLastInteraction;
    private Double affinityScore;
}
```

### 4.5 EngagementFeatures.java

```java
package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EngagementFeatures {
    private Long popularity;
    private Long upvoteCount;
    private Long downvoteCount;
    private Long commentCount;
    private Long shareCount;
    private Long viewCount;
}
```

---

## Testing Strategy

### Unit Tests

```java
@Test
public void testExtractHashtags() {
    String content = "Check out this #amazing #AI project! #MachineLearning";
    List<String> hashtags = contentAnalysisService.extractHashtags(content);
    
    assertEquals(3, hashtags.size());
    assertTrue(hashtags.contains("amazing"));
    assertTrue(hashtags.contains("ai"));
    assertTrue(hashtags.contains("machinelearning"));
}

@Test
public void testCalculateKeywordRelevance() {
    List<String> postKeywords = Arrays.asList("machine", "learning", "ai", "model");
    Map<String, Double> userProfile = Map.of(
        "machine", 0.8,
        "learning", 0.9,
        "ai", 1.0,
        "data", 0.7
    );
    
    double relevance = userInterestProfileService.calculateKeywordRelevance(
        postKeywords, userProfile
    );
    
    assertTrue(relevance > 0);
    assertTrue(relevance <= 1000);
}
```

---

## Performance Considerations

### Caching Strategy

1. **User Interest Profiles**: Cache for 1 hour
   - Key: `user:keyword_profile:{userId}`
   - Key: `user:hashtag_profile:{userId}`

2. **Author Features**: Cache for 10 minutes
   - Key: `author:features:{authorId}`

3. **Post Content Analysis**: Cache for 24 hours
   - Key: `post:content_analysis:{postId}`

### Batch Processing

For feed generation, extract features in batches:
- Process 50-100 posts at once
- Use parallel streams for independent calculations
- Pre-fetch user profiles before processing candidates

### Database Optimization

Add indexes:
```sql
CREATE INDEX idx_user_behavior_user_time ON user_behavior(user_id, event_time DESC);
CREATE INDEX idx_user_behavior_post ON user_behavior(post_id);
CREATE INDEX idx_follow_follower ON follow(follower_id);
CREATE INDEX idx_follow_following ON follow(following_id);
```

---

## Next Steps

1. ✅ Review this feature extraction plan
2. ❌ Implement `ContentAnalysisService`
3. ❌ Implement `UserInterestProfileService`
4. ❌ Create new DTO classes
5. ❌ Update `FeatureExtractionService`
6. ❌ Write unit tests
7. ❌ Add caching layer
8. ❌ Test with sample data

Estimated implementation time: 3-5 days
