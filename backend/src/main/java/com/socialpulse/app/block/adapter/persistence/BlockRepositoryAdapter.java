package com.socialpulse.app.block.adapter.persistence;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

import com.socialpulse.app.block.domain.model.Block;
import com.socialpulse.app.block.domain.repository.BlockRepository;
import com.socialpulse.app.block.infrastructure.persistence.entity.BlockEntity;
import com.socialpulse.app.block.infrastructure.persistence.mapper.BlockPersistenceMapper;
import com.socialpulse.app.block.infrastructure.persistence.repository.JpaBlockRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Repository
public class BlockRepositoryAdapter implements BlockRepository {
    private final JpaBlockRepository jpaBlockRepository;
    private final JpaUserRepository jpaUserRepository;
    private final BlockPersistenceMapper mapper;

    public BlockRepositoryAdapter(JpaBlockRepository jpaBlockRepository,
                                  JpaUserRepository jpaUserRepository,
                                  BlockPersistenceMapper mapper) {
        this.jpaBlockRepository = jpaBlockRepository;
        this.jpaUserRepository = jpaUserRepository;
        this.mapper = mapper;
    }

    @Override
    public Block save(Block block) {
        UserEntity blocker = jpaUserRepository.findById(block.getBlockerId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));
        UserEntity blocked = jpaUserRepository.findById(block.getBlockedId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        BlockEntity entity = mapper.toEntity(block, blocker, blocked);
        BlockEntity saved = jpaBlockRepository.save(entity);
        return mapper.toDomain(saved);
    }

    @Override
    public Optional<Block> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return jpaBlockRepository.findByBlockerIdAndBlockedId(blockerId, blockedId)
                .map(mapper::toDomain);
    }

    @Override
    public void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        jpaBlockRepository.deleteByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId) {
        return jpaBlockRepository.existsByBlockerIdAndBlockedId(blockerId, blockedId);
    }

    @Override
    public List<Block> findByBlockerId(Long blockerId) {
        return jpaBlockRepository.findByBlockerId(blockerId).stream()
                .map(mapper::toDomain)
                .toList();
    }

    @Override
    public List<Block> findByBlockedId(Long blockedId) {
        return jpaBlockRepository.findByBlockedId(blockedId).stream()
                .map(mapper::toDomain)
                .toList();
    }
}
