package com.socialpulse.app.block.application.usecase;

public interface IsBlockedUseCase {
    boolean isBlocked(Long blockerId, Long blockedId);
}
