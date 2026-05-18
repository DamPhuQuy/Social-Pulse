package com.socialpulse.app.discovery.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.discovery.application.usecase.ClearSearchHistoryUseCase;
import com.socialpulse.app.discovery.domain.repository.SearchHistoryRepository;

@Service
public class ClearSearchHistoryService implements ClearSearchHistoryUseCase {

    private final SearchHistoryRepository searchHistoryRepository;

    public ClearSearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Override
    @Transactional
    public void clearSearchHistory(Long userId) {
        searchHistoryRepository.deleteByUserId(userId);
    }
}
