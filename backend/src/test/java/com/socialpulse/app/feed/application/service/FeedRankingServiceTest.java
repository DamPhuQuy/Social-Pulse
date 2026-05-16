package com.socialpulse.app.feed.application.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.usecase.CacheFeedUseCase;
import com.socialpulse.app.feed.application.usecase.ExtractFeaturesUseCase;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;
import com.socialpulse.app.feed.application.usecase.SelectCandidatesUseCase;
import com.socialpulse.app.feed.domain.enums.Source;
import com.socialpulse.app.feed.domain.model.CandidatePost;
import com.socialpulse.app.feed.domain.model.FeedItem;
import com.socialpulse.app.post.domain.model.Post;

@ExtendWith(MockitoExtension.class)
class FeedRankingServiceTest {
    @Mock
    private SelectCandidatesUseCase selectCandidatesUseCase;

    @Mock
    private ExtractFeaturesUseCase extractFeaturesUseCase;

    @Mock
    private PredictRankingUseCase predictRankingUseCase;

    @Mock
    private CacheFeedUseCase cacheFeedUseCase;

    @Test
    void fallsBackToDeterministicRankingWhenModelPredictionIsUnavailable() {
        List<CandidatePost> candidates = List.of(
                CandidatePost.builder()
                        .post(Post.builder()
                                .id(100L)
                                .hotScore(10.0)
                                .cmtCount(20L)
                                .shareCount(5L)
                                .viewCount(100L)
                                .createdAt(LocalDateTime.now().minusHours(1))
                                .build())
                        .source(Source.POPULAR)
                        .build(),
                CandidatePost.builder()
                        .post(Post.builder()
                                .id(200L)
                                .hotScore(1.0)
                                .cmtCount(1L)
                                .shareCount(0L)
                                .viewCount(10L)
                                .createdAt(LocalDateTime.now().minusHours(12))
                                .build())
                        .source(Source.RECENT)
                        .build());
        List<RankingFeatures> features = List.of(
                RankingFeatures.builder().postId(100L).build(),
                RankingFeatures.builder().postId(200L).build());

        when(cacheFeedUseCase.getCachedFeed(42L)).thenReturn(null);
        when(selectCandidatesUseCase.selectCandidates(42L)).thenReturn(candidates);
        when(extractFeaturesUseCase.extractFeatures(42L, candidates)).thenReturn(features);
        when(predictRankingUseCase.predictScores(any())).thenReturn(List.of());

        FeedRankingService service = new FeedRankingService(
                selectCandidatesUseCase,
                extractFeaturesUseCase,
                predictRankingUseCase,
                cacheFeedUseCase,
                new FallbackRankingService("v1"),
                "v1");

        List<FeedItem> rankedFeed = service.getRankedFeed(42L);

        assertEquals(2, rankedFeed.size());
        assertEquals(100L, rankedFeed.get(0).getPostId());
        assertEquals(200L, rankedFeed.get(1).getPostId());
        verify(predictRankingUseCase).predictScores(any());
        verify(cacheFeedUseCase).cacheFeed(eq(42L), any());
    }

    @Test
    void usesPredictedScoresWhenPredictionSetIsValid() {
        List<CandidatePost> candidates = List.of(
                CandidatePost.builder()
                        .post(Post.builder().id(100L).createdAt(LocalDateTime.now().minusHours(3)).build())
                        .source(Source.RECENT)
                        .build(),
                CandidatePost.builder()
                        .post(Post.builder().id(200L).createdAt(LocalDateTime.now().minusHours(3)).build())
                        .source(Source.POPULAR)
                        .build());
        List<RankingFeatures> features = List.of(
                RankingFeatures.builder().postId(100L).build(),
                RankingFeatures.builder().postId(200L).build());
        List<RankingResponse> predictions = List.of(
                RankingResponse.builder().postId(100L).score(0.1).featureSchemaVersion("v1").build(),
                RankingResponse.builder().postId(200L).score(0.9).featureSchemaVersion("v1").build());

        when(cacheFeedUseCase.getCachedFeed(42L)).thenReturn(null);
        when(selectCandidatesUseCase.selectCandidates(42L)).thenReturn(candidates);
        when(extractFeaturesUseCase.extractFeatures(42L, candidates)).thenReturn(features);
        when(predictRankingUseCase.predictScores(any())).thenReturn(predictions);

        FeedRankingService service = new FeedRankingService(
                selectCandidatesUseCase,
                extractFeaturesUseCase,
                predictRankingUseCase,
                cacheFeedUseCase,
                new FallbackRankingService("v1"),
                "v1");

        var rankedFeed = service.getRankedFeed(42L);

        assertEquals(2, rankedFeed.size());
        assertEquals(200L, rankedFeed.get(0).getPostId());
        assertEquals(100L, rankedFeed.get(1).getPostId());
    }
}
