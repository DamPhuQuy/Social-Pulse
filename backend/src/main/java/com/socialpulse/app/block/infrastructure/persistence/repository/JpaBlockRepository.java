package com.socialpulse.app.block.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.block.infrastructure.persistence.entity.BlockEntity;

@Repository
public interface JpaBlockRepository extends JpaRepository<BlockEntity, Long> {
    boolean existsByBlockerIdAndBlockedId(Long blockerId, Long blockedId);
    
    Optional<BlockEntity> findByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    void deleteByBlockerIdAndBlockedId(Long blockerId, Long blockedId);

    List<BlockEntity> findByBlockerId(Long blockerId);

    List<BlockEntity> findByBlockedId(Long blockedId);
}
