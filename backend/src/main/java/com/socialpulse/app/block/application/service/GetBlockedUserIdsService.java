package com.socialpulse.app.block.application.service;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.block.application.usecase.GetBlockedUserIdsUseCase;
import com.socialpulse.app.block.application.usecase.GetUsersWhoBlockedMeUseCase;
import com.socialpulse.app.block.domain.model.Block;
import com.socialpulse.app.block.domain.repository.BlockRepository;

@Service
public class GetBlockedUserIdsService implements GetBlockedUserIdsUseCase, GetUsersWhoBlockedMeUseCase {
    private final BlockRepository blockRepository;

    public GetBlockedUserIdsService(BlockRepository blockRepository) {
        this.blockRepository = blockRepository;
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findByBlockerId(blockerId).stream()
                .map(Block::getBlockedId)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Long> getUsersWhoBlockedMe(Long blockedId) {
        return blockRepository.findByBlockedId(blockedId).stream()
                .map(Block::getBlockerId)
                .collect(Collectors.toList());
    }
}
