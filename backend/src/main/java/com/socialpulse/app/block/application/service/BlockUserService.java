package com.socialpulse.app.block.application.service;

import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.block.application.usecase.BlockUserUseCase;
import com.socialpulse.app.block.domain.model.Block;
import com.socialpulse.app.block.domain.repository.BlockRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class BlockUserService implements BlockUserUseCase {
    private final BlockRepository blockRepository;
    private final UserRepository userRepository;
    private final FollowRepository followRepository;

    public BlockUserService(BlockRepository blockRepository,
                            UserRepository userRepository,
                            FollowRepository followRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    @Override
    @Transactional
    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new AppException(UserCode.CANNOT_BLOCK_YOURSELF);
        }

        userRepository.findById(blockerId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        userRepository.findById(blockedId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new AppException(UserCode.ALREADY_BLOCKED);
        }

        Block block = Block.builder()
                .blockerId(blockerId)
                .blockedId(blockedId)
                .build();
        blockRepository.save(block);

        followRepository.deleteByFollowerIdAndFollowingId(blockerId, blockedId);
        followRepository.deleteByFollowerIdAndFollowingId(blockedId, blockerId);
    }
}
