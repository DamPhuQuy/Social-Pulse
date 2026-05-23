package com.socialpulse.app.block.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.block.application.usecase.IsBlockedEitherUseCase;
import com.socialpulse.app.block.application.usecase.IsBlockedUseCase;
import com.socialpulse.app.block.domain.repository.BlockRepository;

public class IsBlockedService implements IsBlockedUseCase, IsBlockedEitherUseCase {
    private final BlockRepository blockRepository;

    public IsBlockedService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean isBlockedEither(Long userA, Long userB) {
        if (userA == null || userB == null) return false;
        return blockRepository.existsByBlockerIdAndBlockedId(userA, userB) ||
               blockRepository.existsByBlockerIdAndBlockedId(userB, userA);
    }
}
