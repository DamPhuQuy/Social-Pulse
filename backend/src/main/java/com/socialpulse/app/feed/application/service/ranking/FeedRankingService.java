package com.socialpulse.app.feed.application.service.ranking;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.socialpulse.app.feed.application.dto.response.RankingResponse;
import com.socialpulse.app.feed.application.usecase.cache.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.candidate.SelectCandidatesUseCase;
import com.socialpulse.app.feed.application.usecase.ranking.RankFeedUseCase;
import com.socialpulse.app.feed.domain.enums.RankingProvider;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class FeedRankingService implements RankFeedUseCase {
    private final SelectCandidatesUseCase selectCandidates;
    private final CacheFeedUseCase cacheFeed;
    private final RuleBasedRankingService ruleBasedRanking;
    private final ScoreBoostService scoreBoost;
    private final String featureSchemaVersion;

    public FeedRankingService(
            SelectCandidatesUseCase selectCandidates,
            CacheFeedUseCase cacheFeed,
            RuleBasedRankingService ruleBasedRanking,
            ScoreBoostService scoreBoost,
            @Value("${feed.ranking.schema-version:v2}") String featureSchemaVersion) {
        this.selectCandidates = selectCandidates;
        this.cacheFeed = cacheFeed;
        this.ruleBasedRanking = ruleBasedRanking;
        this.scoreBoost = scoreBoost;
        this.featureSchemaVersion = featureSchemaVersion;
    }

    @Override
    public List<FeedItem> getRankedFeed(Long userId) {
        List<FeedItem> cached = cacheFeed.getCachedFeed(userId);
        if (cached != null && !cached.isEmpty()) return cached;

        List<CandidatePost> candidates = selectCandidates.selectCandidates(userId);
        List<FeedItem> ranked = processRanking(userId, candidates);
        cacheFeed.cacheFeed(userId, ranked);
        return ranked;
    }

    @Override
    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size) {
        List<FeedItem> all = getRankedFeed(userId);
        return paginate(all, page, size);
    }

    @Override
    public List<FeedItem> getPaginatedFeed(Long userId, int page, int size, String topicSlug) {
        List<CandidatePost> candidates = selectCandidates.selectCandidatesByTopic(topicSlug);
        List<FeedItem> ranked = processRanking(userId, candidates);
        return paginate(ranked, page, size);
    }

    private List<FeedItem> processRanking(Long userId, List<CandidatePost> candidates) {
        if (candidates.isEmpty()) return List.of();

        List<RankingResponse> scores = ruleBasedRanking.rank(candidates);
        Map<Long, CandidatePost> candidateMap = candidates.stream()
                .collect(Collectors.toMap(c -> c.getPost().getId(), c -> c));

        return scores.stream()
                .map(score -> {
                    CandidatePost candidate = candidateMap.get(score.getPostId());
                    double boosted = scoreBoost.boost(score.getScore() != null ? score.getScore() : 0.0, userId, candidate);
                    return FeedItem.builder()
                            .postId(score.getPostId())
                            .userId(userId)
                            .rankingScore(boosted)
                            .source(candidate != null ? candidate.getSource() : null)
                            .rankingProvider(RankingProvider.RULE_BASED)
                            .featureSchemaVersion(featureSchemaVersion)
                            .rankedAt(LocalDateTime.now())
                            .affinityScore(0.0)
                            .interactionCount30d(0L)
                            .build();
                })
                .sorted(
                        Comparator.comparing(FeedItem::getRankingScore, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(FeedItem::getRankedAt, Comparator.nullsLast(Comparator.reverseOrder()))
                                .thenComparing(FeedItem::getPostId, Comparator.nullsLast(Comparator.reverseOrder()))
                )
                .toList();
    }

    private List<FeedItem> paginate(List<FeedItem> items, int page, int size) {
        int start = page * size;
        if (start >= items.size()) return List.of();
        return items.subList(start, Math.min(start + size, items.size()));
    }
}
