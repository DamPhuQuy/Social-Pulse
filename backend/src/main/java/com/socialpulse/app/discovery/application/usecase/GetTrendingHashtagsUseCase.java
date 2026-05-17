package com.socialpulse.app.discovery.application.usecase;

import java.util.List;

import com.socialpulse.app.discovery.application.dto.response.TrendingHashtagResponse;

public interface GetTrendingHashtagsUseCase {
    List<TrendingHashtagResponse> getTrendingHashtags(int days, int limit);
}
