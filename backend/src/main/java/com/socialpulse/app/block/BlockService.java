package com.socialpulse.app.block;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.follow.infrastructure.persistence.repository.JpaFollowRepository;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Service
@Transactional
public class BlockService {

    private final JpaBlockRepository blockRepository;
    private final JpaUserRepository userRepository;
    private final JpaFollowRepository followRepository;

    public BlockService(JpaBlockRepository blockRepository,
                        JpaUserRepository userRepository,
                        JpaFollowRepository followRepository) {
        this.blockRepository = blockRepository;
        this.userRepository = userRepository;
        this.followRepository = followRepository;
    }

    public void blockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new AppException(UserCode.CANNOT_BLOCK_YOURSELF);
        }

        UserEntity blocker = userRepository.findById(blockerId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        UserEntity blocked = userRepository.findById(blockedId)
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        if (blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new AppException(UserCode.ALREADY_BLOCKED);
        }

        // 1. Save block entry
        BlockEntity block = BlockEntity.builder()
                .blocker(blocker)
                .blocked(blocked)
                .build();
        blockRepository.save(block);

        // 2. Automatically remove follow relationship A -> B and B -> A if they exist
        followRepository.deleteByFollowerIdAndFollowingId(blockerId, blockedId);
        followRepository.deleteByFollowerIdAndFollowingId(blockedId, blockerId);
    }

    public void unblockUser(Long blockerId, Long blockedId) {
        if (blockerId.equals(blockedId)) {
            throw new IllegalArgumentException("Cannot unblock yourself");
        }

        if (!blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId)) {
            throw new AppException(UserCode.NOT_BLOCKED);
        }

        blockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean isBlocked(Long blockerId, Long blockedId) {
        return blockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Transactional(readOnly = true)
    public boolean isBlockedEither(Long userA, Long userB) {
        if (userA == null || userB == null) return false;
        return blockRepository.existsByBlockerIdAndBlockedId(userA, userB) ||
               blockRepository.existsByBlockerIdAndBlockedId(userB, userA);
    }

    @Transactional(readOnly = true)
    public List<Long> getBlockedUserIds(Long blockerId) {
        return blockRepository.findByBlockerId(blockerId).stream()
                .map(block -> block.getBlocked().getId())
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public List<Long> getUsersWhoBlockedMe(Long blockedId) {
        return blockRepository.findByBlockedId(blockedId).stream()
                .map(block -> block.getBlocker().getId())
                .collect(Collectors.toList());
    }
}
