package com.socialpulse.app.block.application.usecase;

public interface UnblockUserUseCase {
    void unblockUser(Long blockerId, Long blockedId);
}
