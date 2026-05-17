package com.socialpulse.app.feed.application.usecase;

import java.util.List;

import com.socialpulse.app.feed.application.dto.features.RankingRequest;
import com.socialpulse.app.feed.application.dto.features.RankingResponse;

public interface PredictRankingUseCase {
    List<RankingResponse> predictScores(RankingRequest request);
}
