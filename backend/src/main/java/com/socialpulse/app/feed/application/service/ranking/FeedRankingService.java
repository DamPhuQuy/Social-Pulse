package com.socialpulse.app.feed.application.service.ranking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import com.socialpulse.app.feed.application.dto.features.core.RankingFeatures;
import com.socialpulse.app.feed.application.dto.request.RankingRequest;
import com.socialpulse.app.feed.application.dto.response.RankingResponse;
import com.socialpulse.app.feed.application.usecase.cache.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.candidate.SelectCandidatesUseCase;
import com.socialpulse.app.feed.application.usecase.extraction.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.RankFeedUseCase;
import com.socialpulse.app.feed.domain.enums.RankingProvider;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class FeedRankingService implements RankFeedUseCase {
    private final SelectCandidatesUseCase selectCandidates;
    private final ExtractFeaturesUseCase extractFeatures;
    private final PredictRankingUseCase predictRanking;
    private final CacheFeedUseCase cacheFeed;
    private final FallbackRankingService fallback;
    private final ScoreBoostService scoreBoost;
    private final String featureSchemaVersion;

    public FeedRankingService(
            SelectCandidatesUseCase selectCandidates,
            ExtractFeaturesUseCase extractFeatures,
            PredictRankingUseCase predictRanking,
            CacheFeedUseCase cacheFeed,
            FallbackRankingService fallback,
            ScoreBoostService scoreBoost,
            String featureSchemaVersion) {
        this.selectCandidates = selectCandidates;
        this.extractFeatures = extractFeatures;
        this.predictRanking = predictRanking;
        this.cacheFeed = cacheFeed;
        this.fallback = fallback;
        this.scoreBoost = scoreBoost;
        this.featureSchemaVersion = featureSchemaVersion;
    }

    @Override
    public List<FeedItem> getRankedFeed(Long userId) {
        List<FeedItem> cached = cacheFeed.getCachedFeed(userId);
        if (cached != null && !cached.isEmpty()) return cached;

        List<CandidatePost> candidates = selectCandidates.selectCandidates(userId);
        if (candidates.isEmpty()) return List.of();

        ScoreResolution resolution = resolveScores(userId, candidates);
        Map<Long, CandidatePost> candidateMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getPost().getId(), c -> c));

        List<FeedItem> ranked = resolution.scores().stream()
                .map(score -> {
                    CandidatePost candidate = candidateMap.get(score.getPostId());
                    double boosted = scoreBoost.boost(score.getScore() != null ? score.getScore() : 0.0, userId, candidate);
                    return FeedItem.builder()
                            .postId(score.getPostId())
                            .userId(userId)
                            .aiScore(boosted)
                            .source(candidate.getSource())
                            .rankingProvider(resolution.provider())
                            .featureSchemaVersion(featureSchemaVersion)
                            .rankedAt(LocalDateTime.now())
                            .build();
                })
                .sorted(Comparator.comparing(FeedItem::getAiScore).reversed())
                .toList();

        cacheFeed.cacheFeed(userId, ranked);
        return ranked;
    }

    @Override
    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size) {
        List<FeedItem> all = getRankedFeed(userId);
        int start = page * size;
        if (start >= all.size()) return List.of();
        return all.subList(start, Math.min(start + size, all.size()));
    }

    @Override
    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size, String topicSlug) {
        List<CandidatePost> candidates = selectCandidates.selectCandidatesByTopic(topicSlug);
        if (candidates.isEmpty()) return List.of();

        ScoreResolution resolution = resolveScores(userId, candidates);
        Map<Long, CandidatePost> candidateMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getPost().getId(), c -> c));

        List<FeedItem> ranked = resolution.scores().stream()
                .map(score -> {
                    CandidatePost candidate = candidateMap.get(score.getPostId());
                    double boosted = scoreBoost.boost(score.getScore() != null ? score.getScore() : 0.0, userId, candidate);
                    return FeedItem.builder()
                            .postId(score.getPostId())
                            .userId(userId)
                            .aiScore(boosted)
                            .source(candidate.getSource())
                            .rankingProvider(resolution.provider())
                            .featureSchemaVersion(featureSchemaVersion)
                            .rankedAt(LocalDateTime.now())
                            .build();
                })
                .sorted(Comparator.comparing(FeedItem::getAiScore).reversed())
                .toList();

        int start = page * size;
        if (start >= ranked.size()) return List.of();
        return ranked.subList(start, Math.min(start + size, ranked.size()));
    }

    private ScoreResolution resolveScores(Long userId, List<CandidatePost> candidates) {
        List<RankingFeatures> features = extractFeatures.extractFeatures(userId, candidates);
        if (!features.isEmpty()) {
            List<RankingResponse> predicted = predictRanking.predictScores(
                    RankingRequest.builder().featureSchemaVersion(featureSchemaVersion).features(features).build());
            if (isValid(predicted, candidates)) return new ScoreResolution(predicted, RankingProvider.AI);
            log.warn("AI prediction invalid for userId={}, reason={}, using fallback",
                    userId, validationFailureReason(predicted, candidates));
        }
        return new ScoreResolution(fallback.rank(candidates), RankingProvider.FALLBACK);
    }

    private boolean isValid(List<RankingResponse> scores, List<CandidatePost> candidates) {
        if (scores == null || scores.isEmpty()) return false;
        Set<Long> ids = candidates.stream().map(c -> c.getPost().getId()).collect(Collectors.toSet());
        if (scores.size() != ids.size()) return false;
        return scores.stream().allMatch(s ->
                s.getPostId() != null && s.getScore() != null
                        && featureSchemaVersion.equals(s.getFeatureSchemaVersion())
                        && ids.contains(s.getPostId()));
    }

    private String validationFailureReason(List<RankingResponse> scores, List<CandidatePost> candidates) {
        if (scores == null) return "null response from AI";
        if (scores.isEmpty()) return "empty response from AI";

        Set<Long> ids = candidates.stream().map(c -> c.getPost().getId()).collect(Collectors.toSet());
        if (scores.size() != ids.size()) {
            return "response size " + scores.size() + " does not match candidate size " + ids.size();
        }

        return scores.stream()
                .filter(s -> s.getPostId() == null || s.getScore() == null
                        || !featureSchemaVersion.equals(s.getFeatureSchemaVersion())
                        || !ids.contains(s.getPostId()))
                .findFirst()
                .map(s -> "invalid row postId=" + s.getPostId()
                        + ", score=" + s.getScore()
                        + ", schema=" + s.getFeatureSchemaVersion())
                .orElse("unknown validation mismatch");
    }

    private record ScoreResolution(List<RankingResponse> scores, RankingProvider provider) {
    }
}
