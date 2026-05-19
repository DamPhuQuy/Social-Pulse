package com.socialpulse.app.discovery.application.usecase;

import java.util.List;

import com.socialpulse.app.discovery.application.dto.response.SearchHistoryResponse;

public interface GetSearchHistoryUseCase {
    List<SearchHistoryResponse> getSearchHistory(Long userId);
}
