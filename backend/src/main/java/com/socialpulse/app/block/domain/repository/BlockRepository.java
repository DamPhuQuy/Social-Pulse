package com.socialpulse.app.block.domain.repository;

import java.util.List;
import java.util.Optional;

import com.socialpulse.app.block.domain.model.Block;

public interface BlockRepository {
    Block save(Block block);

    Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<Block> findByBlockerId(Long blockerId);

    List<Block> findByBlockedId(Long blockedId);
}
