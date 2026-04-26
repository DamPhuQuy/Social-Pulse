package com.socialpulse.app.behavior.application.usecase;

import com.socialpulse.app.behavior.application.dto.UserInteractionFeatures;

import java.util.List;

public interface ExtractBehaviorFeaturesUseCase {
    List<UserInteractionFeatures> extractFeatures(Long userId, List<Long> authorIds);
}
