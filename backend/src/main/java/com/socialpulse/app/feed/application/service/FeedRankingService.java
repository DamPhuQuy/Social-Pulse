package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;

@Service
public class FeedRankingService {
    private final CandidateSelectionService candidateSelectionService;
    private final FeatureExtractionService featureExtractionService;
    private final AiRankingService aiRankingService;
    private final FeedCacheService feedCacheService;

    public FeedRankingService(
            CandidateSelectionService candidateSelectionService,
            FeatureExtractionService featureExtractionService,
            AiRankingService aiRankingService,
            FeedCacheService feedCacheService) {
        this.candidateSelectionService = candidateSelectionService;
        this.featureExtractionService = featureExtractionService;
        this.aiRankingService = aiRankingService;
        this.feedCacheService = feedCacheService;
    }

    public List<FeedItem> getRankedFeed(Long userId) {
        List<FeedItem> cachedFeed = feedCacheService.getCachedFeed(userId);
        if (cachedFeed != null && !cachedFeed.isEmpty()) {
            return cachedFeed;
        }

        List<CandidatePost> candidates = candidateSelectionService.selectCandidates(userId);

        if (candidates.isEmpty()) {
            return List.of();
        }

        List<RankingFeatures> features = featureExtractionService.extractFeatures(userId, candidates);

        RankingRequest request = RankingRequest.builder()
                .features(features)
                .build();

        List<RankingResponse> scores = aiRankingService.predictScores(request);

        Map<Long, Double> scoreMap = scores.stream()
                .collect(Collectors.toMap(RankingResponse::getPostId, RankingResponse::getScore));

        Map<Long, String> sourceMap = candidates.stream()
                .collect(Collectors.toMap(
                        candidate -> candidate.getPost().getId(),
                        CandidatePost::getSource
                ));

        List<FeedItem> rankedFeed = scores.stream()
                .sorted(Comparator.comparing(RankingResponse::getScore).reversed())
                .map(score -> FeedItem.builder()
                        .postId(score.getPostId())
                        .userId(userId)
                        .aiScore(score.getScore())
                        .source(sourceMap.get(score.getPostId()))
                        .rankedAt(LocalDateTime.now())
                        .build())
                .collect(Collectors.toList());

        feedCacheService.cacheFeed(userId, rankedFeed);

        return rankedFeed;
    }

    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size) {
        List<FeedItem> allFeed = getRankedFeed(userId);

        int start = page * size;
        int end = Math.min(start + size, allFeed.size());

        if (start >= allFeed.size()) {
            return List.of();
        }

        return allFeed.subList(start, end);
    }
}
