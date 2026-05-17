package com.socialpulse.app.feed.application.service;

import java.util.List;

import com.socialpulse.app.feed.application.dto.RankingRequest;
import com.socialpulse.app.feed.application.dto.RankingResponse;
import com.socialpulse.app.feed.application.usecase.PredictRankingUseCase;

public class DisabledPredictRankingService implements PredictRankingUseCase {
    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        return List.of();
    }
}
