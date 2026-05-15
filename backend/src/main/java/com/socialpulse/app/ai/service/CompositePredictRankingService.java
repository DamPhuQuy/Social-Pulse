package com.socialpulse.app.ai.service;

import java.util.List;

import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.service.AiRankingService;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;

public class CompositePredictRankingService implements PredictRankingUseCase {
    private final LightGbmRankingService lightGbmRankingService;
    private final AiRankingService aiRankingService;

    public CompositePredictRankingService(
            LightGbmRankingService lightGbmRankingService,
            AiRankingService aiRankingService) {
        this.lightGbmRankingService = lightGbmRankingService;
        this.aiRankingService = aiRankingService;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        List<RankingResponse> lightGbmScores = lightGbmRankingService.predictScores(request);
        if (!lightGbmScores.isEmpty()) {
            return lightGbmScores;
        }
        return aiRankingService.predictScores(request);
    }
}
