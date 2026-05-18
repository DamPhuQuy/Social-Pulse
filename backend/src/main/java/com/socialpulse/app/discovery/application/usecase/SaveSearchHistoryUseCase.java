package com.socialpulse.app.discovery.application.usecase;

import com.socialpulse.app.discovery.application.dto.request.SaveSearchHistoryRequest;

public interface SaveSearchHistoryUseCase {
    void saveSearchHistory(Long userId, SaveSearchHistoryRequest request);
}
