package com.socialpulse.app.discovery.domain.repository;

import java.util.List;
import java.util.Optional;

import com.socialpulse.app.discovery.domain.model.SearchHistory;

public interface SearchHistoryRepository {
    Optional<SearchHistory> findByUserIdAndKeyword(Long userId, String keyword);
    List<SearchHistory> findByUserIdOrderByUpdatedAtDesc(Long userId);
    SearchHistory save(SearchHistory searchHistory);
    void deleteById(Long id);
    void deleteByUserId(Long userId);
    int countByUserId(Long userId);
    Optional<SearchHistory> findOldestByUserId(Long userId);
    Optional<SearchHistory> findByIdAndUserId(Long id, Long userId);
}
