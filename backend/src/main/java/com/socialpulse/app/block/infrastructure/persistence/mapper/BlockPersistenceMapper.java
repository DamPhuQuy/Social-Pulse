package com.socialpulse.app.block.infrastructure.persistence.mapper;

import org.mapstruct.Builder;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.ReportingPolicy;

import com.socialpulse.app.block.domain.model.Block;
import com.socialpulse.app.block.infrastructure.persistence.entity.BlockEntity;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Mapper(componentModel = "spring", unmappedTargetPolicy = ReportingPolicy.IGNORE, builder = @Builder)
public interface BlockPersistenceMapper {
    @Mapping(target = "blocker", ignore = true)
    @Mapping(target = "blocked", ignore = true)
    BlockEntity toEntity(Block block);

    @Mapping(target = "blockerId", source = "blocker.id")
    @Mapping(target = "blockedId", source = "blocked.id")
    Block toDomain(BlockEntity blockEntity);

    default BlockEntity toEntity(Block block, UserEntity blocker, UserEntity blocked) {
        if (block == null) {
            return null;
        }
        BlockEntity entity = new BlockEntity();
        entity.setId(block.getId());
        entity.setBlocker(blocker);
        entity.setBlocked(blocked);
        entity.setCreatedAt(block.getCreatedAt());
        return entity;
    }
}
