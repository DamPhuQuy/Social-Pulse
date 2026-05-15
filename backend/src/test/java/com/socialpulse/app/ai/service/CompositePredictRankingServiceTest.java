package com.socialpulse.app.ai.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.core.io.DefaultResourceLoader;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.socialpulse.app.ai.config.LightGbmProperties;
import com.socialpulse.app.ai.lightgbm.LightGbmFeatureVectorizer;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;
import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.dto.UserFeatures;
import com.socialpulse.app.feed.application.service.AiRankingService;

class CompositePredictRankingServiceTest {
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Test
    void fallsBackToHttpServiceWhenLightGbmIsDisabled() {
        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(false);
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService lightGbmRankingService = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        AiRankingService aiRankingService = Mockito.mock(AiRankingService.class);
        when(aiRankingService.predictScores(Mockito.any())).thenReturn(List.of(
                RankingResponse.builder().postId(200L).score(0.77).featureSchemaVersion("v1").build()));

        CompositePredictRankingService service = new CompositePredictRankingService(lightGbmRankingService, aiRankingService);
        List<RankingResponse> responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(1L)))
                .build());

        assertEquals(1, responses.size());
        assertEquals(200L, responses.get(0).getPostId());
        verify(aiRankingService).predictScores(Mockito.any());
    }

    @Test
    void fallsBackToHttpServiceWhenLightGbmModelCannotBeLoaded() {
        LightGbmProperties properties = new LightGbmProperties();
        properties.setEnabled(true);
        properties.setModelLocation("classpath:ai/missing-model.json");
        properties.setFeatureSchemaVersion("v1");

        LightGbmRankingService lightGbmRankingService = new LightGbmRankingService(
                properties,
                objectMapper,
                new DefaultResourceLoader(),
                new LightGbmFeatureVectorizer());

        AiRankingService aiRankingService = Mockito.mock(AiRankingService.class);
        when(aiRankingService.predictScores(Mockito.any())).thenReturn(List.of(
                RankingResponse.builder().postId(300L).score(0.88).featureSchemaVersion("v1").build()));

        CompositePredictRankingService service = new CompositePredictRankingService(lightGbmRankingService, aiRankingService);
        List<RankingResponse> responses = service.predictScores(RankingRequest.builder()
                .featureSchemaVersion("v1")
                .features(List.of(rankingFeatures(1L)))
                .build());

        assertEquals(1, responses.size());
        assertEquals(300L, responses.get(0).getPostId());
        verify(aiRankingService).predictScores(Mockito.any());
    }

    private RankingFeatures rankingFeatures(Long postId) {
        return RankingFeatures.builder()
                .postId(postId)
                .postFeatures(PostFeatures.builder()
                        .hotScore(1.0)
                        .upvoteRatio(0.5)
                        .contentLength(50)
                        .postAgeHours(1.0)
                        .build())
                .authorFeatures(UserFeatures.builder()
                        .accountAgeDays(10L)
                        .postCount(2L)
                        .engagementRate(0.0)
                        .build())
                .build();
    }
}
