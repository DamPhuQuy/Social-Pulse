package com.socialpulse.app.discovery.application.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.DiscoveryCode;
import com.socialpulse.app.discovery.application.usecase.DeleteSearchHistoryUseCase;
import com.socialpulse.app.discovery.domain.repository.SearchHistoryRepository;

@Service
public class DeleteSearchHistoryService implements DeleteSearchHistoryUseCase {

    private final SearchHistoryRepository searchHistoryRepository;

    public DeleteSearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Override
    @Transactional
    public void deleteSearchHistory(Long userId, Long historyId) {
        var history = searchHistoryRepository.findByIdAndUserId(historyId, userId)
                .orElseThrow(() -> new AppException(DiscoveryCode.SEARCH_HISTORY_NOT_FOUND));

        searchHistoryRepository.deleteById(history.getId());
    }
}
