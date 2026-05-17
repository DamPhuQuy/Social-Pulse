package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.socialpulse.app.feed.application.dto.features.RankingFeatures;
import com.socialpulse.app.feed.application.dto.features.RankingRequest;
import com.socialpulse.app.feed.application.dto.features.RankingResponse;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.post.domain.model.Post;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeedRankingService implements RankFeedUseCase {
    private final SelectCandidatesUseCase selectCandidatesUseCase;
    private final ExtractFeaturesUseCase extractFeaturesUseCase;
    private final PredictRankingUseCase predictRankingUseCase;
    private final CacheFeedUseCase cacheFeedUseCase;
    private final FallbackRankingService fallbackRankingService;
    private final String featureSchemaVersion;

    public FeedRankingService(
            SelectCandidatesUseCase selectCandidatesUseCase,
            ExtractFeaturesUseCase extractFeaturesUseCase,
            PredictRankingUseCase predictRankingUseCase,
            CacheFeedUseCase cacheFeedUseCase,
            FallbackRankingService fallbackRankingService,
            String featureSchemaVersion) {
        this.selectCandidatesUseCase = selectCandidatesUseCase;
        this.extractFeaturesUseCase = extractFeaturesUseCase;
        this.predictRankingUseCase = predictRankingUseCase;
        this.cacheFeedUseCase = cacheFeedUseCase;
        this.fallbackRankingService = fallbackRankingService;
        this.featureSchemaVersion = featureSchemaVersion;
    }

    @Override
    public List<FeedItem> getRankedFeed(Long userId) {
        List<FeedItem> cachedFeed = cacheFeedUseCase.getCachedFeed(userId);
        if (cachedFeed != null && !cachedFeed.isEmpty()) {
            return cachedFeed;
        }

        List<CandidatePost> candidates = selectCandidatesUseCase.selectCandidates(userId);
        if (candidates.isEmpty()) {
            return List.of();
        }

        List<RankingResponse> scores = fallbackRankingService.rank(candidates);

        List<RankingFeatures> features = extractFeaturesUseCase.extractFeatures(userId, candidates);
        if (!features.isEmpty()) {
            RankingRequest request = RankingRequest.builder()
                    .featureSchemaVersion(featureSchemaVersion)
                    .features(features)
                    .build();

            List<RankingResponse> predictedScores = predictRankingUseCase.predictScores(request);
            if (isValidPredictionSet(predictedScores, candidates)) {
                scores = predictedScores;
            } else {
                log.debug("Falling back to deterministic feed ranking for userId={}", userId);
            }
        }

        Map<Long, CandidatePost> candidateMap = candidates.stream()
                .collect(Collectors.toMap(
                        candidate -> candidate.getPost().getId(),
                        candidate -> candidate
                ));

        List<FeedItem> rankedFeed = scores.stream()
                .map(score -> {
                    Post post = candidateMap.get(score.getPostId()).getPost();
                    double boostedScore = score.getScore() != null ? score.getScore() : 0.0;

                    // CRITICAL UX FIX: AI Models suffer from "Cold Start" problem.
                    // A brand new post has 0 likes/comments/views, so AI ranks it very low,
                    // causing it to be buried under old seed posts.
                    // 1. Creator Boost: Massive +10,000 boost to the user's OWN posts created in the last 60 minutes
                    if (post.getUserId().equals(userId) && post.getCreatedAt() != null) {
                        long ageMinutes = Math.max(0, java.time.Duration.between(post.getCreatedAt(), java.time.LocalDateTime.now()).toMinutes());
                        if (ageMinutes <= 60) {
                            boostedScore += 10000.0;
                        }
                    }
                    // 2. Follower Boost: +5,000 boost for posts from people the user FOLLOWS, if created within 24 hours.
                    // This ensures followers actually see new posts so they can interact with them and train the AI!
                    else if (candidateMap.get(score.getPostId()).getSource() == Source.FOLLOWING && post.getCreatedAt() != null) {
                        long ageHours = Math.max(0, java.time.Duration.between(post.getCreatedAt(), java.time.LocalDateTime.now()).toHours());
                        if (ageHours <= 24) {
                            // Boost decays slightly over 24 hours to keep the very newest ones on top
                            boostedScore += 5000.0 - (ageHours * 50.0);
                        }
                    }

                    return FeedItem.builder()
                            .postId(score.getPostId())
                            .userId(userId)
                            .aiScore(boostedScore)
                            .source(candidateMap.get(score.getPostId()).getSource())
                            .rankedAt(LocalDateTime.now())
                            .build();
                })
                .sorted(Comparator.comparing(FeedItem::getAiScore).reversed())
                .toList();

        cacheFeedUseCase.cacheFeed(userId, rankedFeed);
        return rankedFeed;
    }

    @Override
    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size) {
        List<FeedItem> allFeed = getRankedFeed(userId);

        int start = page * size;
        int end = Math.min(start + size, allFeed.size());

        if (start >= allFeed.size()) {
            return List.of();
        }

        return allFeed.subList(start, end);
    }

    private boolean isValidPredictionSet(List<RankingResponse> predictedScores, List<CandidatePost> candidates) {
        if (predictedScores == null || predictedScores.isEmpty()) {
            return false;
        }

        Set<Long> candidateIds = candidates.stream()
                .map(candidate -> candidate.getPost().getId())
                .collect(Collectors.toSet());

        if (predictedScores.size() != candidateIds.size()) {
            return false;
        }

        return predictedScores.stream().allMatch(score ->
                score.getPostId() != null
                        && score.getScore() != null
                        && featureSchemaVersion.equals(score.getFeatureSchemaVersion())
                        && candidateIds.contains(score.getPostId()));
    }
}
