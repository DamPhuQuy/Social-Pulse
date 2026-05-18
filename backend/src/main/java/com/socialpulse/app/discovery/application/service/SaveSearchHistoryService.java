package com.socialpulse.app.discovery.application.service;

import java.time.LocalDateTime;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.discovery.application.dto.request.SaveSearchHistoryRequest;
import com.socialpulse.app.discovery.application.usecase.SaveSearchHistoryUseCase;
import com.socialpulse.app.discovery.domain.model.SearchHistory;
import com.socialpulse.app.discovery.domain.repository.SearchHistoryRepository;

@Service
public class SaveSearchHistoryService implements SaveSearchHistoryUseCase {

    private static final int MAX_HISTORY_SIZE = 20;
    private final SearchHistoryRepository searchHistoryRepository;

    public SaveSearchHistoryService(SearchHistoryRepository searchHistoryRepository) {
        this.searchHistoryRepository = searchHistoryRepository;
    }

    @Override
    @Transactional
    public void saveSearchHistory(Long userId, SaveSearchHistoryRequest request) {
        String keyword = request.getKeyword();

        // Trim and validate
        keyword = keyword.trim();
        if (keyword.isEmpty()) {
            return;
        }

        // Normalize to lowercase for case-insensitive comparison
        String normalizedKeyword = keyword.toLowerCase();

        // Check if keyword already exists
        var existingHistory = searchHistoryRepository.findByUserIdAndKeyword(userId, normalizedKeyword);

        if (existingHistory.isPresent()) {
            // Update existing record's timestamp
            SearchHistory history = existingHistory.get();
            history.updateSearchTime();
            searchHistoryRepository.save(history);
        } else {
            // Check if user has reached the limit
            int currentCount = searchHistoryRepository.countByUserId(userId);

            if (currentCount >= MAX_HISTORY_SIZE) {
                // Remove the oldest record (FIFO)
                var oldestHistory = searchHistoryRepository.findOldestByUserId(userId);
                oldestHistory.ifPresent(history -> searchHistoryRepository.deleteById(history.getId()));
            }

            // Create new record
            LocalDateTime now = LocalDateTime.now();
            SearchHistory newHistory = SearchHistory.builder()
                    .userId(userId)
                    .keyword(normalizedKeyword)
                    .createdAt(now)
                    .updatedAt(now)
                    .build();

            searchHistoryRepository.save(newHistory);
        }
    }
}
