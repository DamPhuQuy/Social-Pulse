package com.socialpulse.app.feed.application.service;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.RankFeedUseCase;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeedRankingService implements RankFeedUseCase {
    private static final String FEATURE_SCHEMA_VERSION = "v1";

    private final SelectCandidatesUseCase selectCandidatesUseCase;
    private final ExtractFeaturesUseCase extractFeaturesUseCase;
    private final PredictRankingUseCase predictRankingUseCase;
    private final CacheFeedUseCase cacheFeedUseCase;
    private final FallbackRankingService fallbackRankingService;

    public FeedRankingService(
            SelectCandidatesUseCase selectCandidatesUseCase,
            ExtractFeaturesUseCase extractFeaturesUseCase,
            PredictRankingUseCase predictRankingUseCase,
            CacheFeedUseCase cacheFeedUseCase,
            FallbackRankingService fallbackRankingService) {
        this.selectCandidatesUseCase = selectCandidatesUseCase;
        this.extractFeaturesUseCase = extractFeaturesUseCase;
        this.predictRankingUseCase = predictRankingUseCase;
        this.cacheFeedUseCase = cacheFeedUseCase;
        this.fallbackRankingService = fallbackRankingService;
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
                    .featureSchemaVersion(FEATURE_SCHEMA_VERSION)
                    .features(features)
                    .build();

            List<RankingResponse> predictedScores = predictRankingUseCase.predictScores(request);
            if (isValidPredictionSet(predictedScores, candidates)) {
                scores = predictedScores;
            } else {
                log.debug("Falling back to deterministic feed ranking for userId={}", userId);
            }
        }

        Map<Long, Source> sourceMap = candidates.stream()
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
                        && FEATURE_SCHEMA_VERSION.equals(score.getFeatureSchemaVersion())
                        && candidateIds.contains(score.getPostId()));
    }
}
