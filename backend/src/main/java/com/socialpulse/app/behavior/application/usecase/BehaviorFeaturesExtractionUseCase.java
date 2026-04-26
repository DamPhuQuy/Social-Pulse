package com.socialpulse.app.behavior.application.usecase;

import java.util.List;

import com.socialpulse.app.behavior.application.dto.UserInteractionFeatures;

public interface BehaviorFeaturesExtractionUseCase {
    List<UserInteractionFeatures> extractFeatures(Long userId, List<Long> authorIds);
}
