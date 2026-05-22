package com.socialpulse.app.block.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.block.application.usecase.UnblockUserUseCase;
import com.socialpulse.app.block.domain.repository.BlockRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;

public class UnblockUserService implements UnblockUserUseCase {
    private final BlockRepository blockRepository;

    public UnblockUserService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    @Transactional
    public void unblockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot unblock yourself");
        }

        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new AppException(UserCode.NOT_BLOCKED);
        }

        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }
}
