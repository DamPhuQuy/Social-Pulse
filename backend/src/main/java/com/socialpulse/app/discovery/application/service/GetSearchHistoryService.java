package com.socialpulse.app.discovery.application.service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.discovery.application.dto.response.SearchHistoryResponse;
import com.socialpulse.app.discovery.application.usecase.GetSearchHistoryUseCase;
import com.socialpulse.app.discovery.domain.repository.SearchHistoryRepository;

@Service
public class GetSearchHistoryService implements GetSearchHistoryUseCase {

    private final SearchHistoryRepository searchHistoryRepository;

    public GetSearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<SearchHistoryResponse> getSearchHistory(Long userId) {
        return searchHistoryRepository.findByUserIdOrderByUpdatedAtDesc(userId)
                .stream()
                .map(history -> SearchHistoryResponse.builder()
                        .id(history.getId())
                        .keyword(history.getKeyword())
                        .createdAt(history.getCreatedAt())
                        .updatedAt(history.getUpdatedAt())
                        .build())
                .collect(Collectors.toList());
    }
}
