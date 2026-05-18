package com.socialpulse.app.user.application.usecase;

import com.socialpulse.app.user.application.dto.request.UpdateUserTopicsRequest;

public interface UpdateUserTopicsUseCase {
    void updateTopics(Long userId, UpdateUserTopicsRequest request);
}
