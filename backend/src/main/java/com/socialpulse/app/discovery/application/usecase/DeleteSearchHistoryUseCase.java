package com.socialpulse.app.discovery.application.usecase;

public interface DeleteSearchHistoryUseCase {
    void deleteSearchHistory(Long userId, Long historyId);
}
