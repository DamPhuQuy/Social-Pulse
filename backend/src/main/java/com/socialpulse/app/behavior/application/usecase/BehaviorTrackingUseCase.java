package com.socialpulse.app.behavior.application.usecase;

import com.socialpulse.app.behavior.domain.model.UserBehavior;

public interface BehaviorTrackingUseCase {
    UserBehavior trackBehavior(UserBehavior behavior);
}
