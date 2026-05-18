package com.socialpulse.app.feed.application.usecase.ranking;

import java.util.List;

import com.socialpulse.app.feed.application.dto.request.RankingRequest;
import com.socialpulse.app.feed.application.dto.response.RankingResponse;

public interface PredictRankingUseCase {
    List<RankingResponse> predictScores(RankingRequest request);
}
