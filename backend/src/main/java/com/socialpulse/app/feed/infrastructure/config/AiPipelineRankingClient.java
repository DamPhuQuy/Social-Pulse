package com.socialpulse.app.feed.infrastructure.config;

import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.web.client.RestClient;

import com.socialpulse.app.feed.application.dto.request.RankingRequest;
import com.socialpulse.app.feed.application.dto.response.RankingResponse;
import com.socialpulse.app.feed.application.usecase.ranking.PredictRankingUseCase;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class AiPipelineRankingClient implements PredictRankingUseCase {
    private final RestClient restClient;
    private final boolean enabled;

    public AiPipelineRankingClient(String baseUrl, boolean enabled) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.enabled = enabled;
    }

    @Override
    public List<RankingResponse> predictScores(RankingRequest request) {
        if (!enabled) {
            return List.of();
        }
        try {
            List<RankingResponse> response = restClient.post()
                    .uri("/api/ranking/predict")
                    .body(request)
                    .retrieve()
                    .body(new ParameterizedTypeReference<>() {});
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("AI pipeline call failed: {}", e.getMessage());
            return List.of();
        }
    }
}
