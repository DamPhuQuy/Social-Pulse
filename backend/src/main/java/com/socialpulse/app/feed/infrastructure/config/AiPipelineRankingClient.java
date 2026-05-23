package com.socialpulse.app.feed.infrastructure.config;

import java.time.Duration;
import java.util.List;

import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
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
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(2));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .requestFactory(requestFactory)
                .build();
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
                    .body(new ParameterizedTypeReference<List<RankingResponse>>() {});
            return response != null ? response : List.of();
        } catch (Exception e) {
            log.warn("AI pipeline call failed: {}", e.getMessage());
            return List.of();
        }
    }
}
