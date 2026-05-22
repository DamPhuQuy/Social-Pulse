package com.socialpulse.app.block.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.block.adapter.persistence.BlockRepositoryAdapter;
import com.socialpulse.app.block.application.service.BlockUserService;
import com.socialpulse.app.block.application.service.GetBlockedUserIdsService;
import com.socialpulse.app.block.application.service.IsBlockedService;
import com.socialpulse.app.block.application.service.UnblockUserService;
import com.socialpulse.app.block.application.usecase.BlockUserUseCase;
import com.socialpulse.app.block.application.usecase.GetBlockedUserIdsUseCase;
import com.socialpulse.app.block.application.usecase.GetUsersWhoBlockedMeUseCase;
import com.socialpulse.app.block.application.usecase.IsBlockedEitherUseCase;
import com.socialpulse.app.block.application.usecase.IsBlockedUseCase;
import com.socialpulse.app.block.application.usecase.UnblockUserUseCase;
import com.socialpulse.app.block.domain.repository.BlockRepository;
import com.socialpulse.app.block.infrastructure.persistence.mapper.BlockPersistenceMapper;
import com.socialpulse.app.block.infrastructure.persistence.repository.JpaBlockRepository;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Configuration
public class BlockConfig {

    @Bean
    public BlockRepository blockRepository(JpaBlockRepository jpaBlockRepository,
                                           JpaUserRepository jpaUserRepository,
                                           BlockPersistenceMapper mapper) {
        return new BlockRepositoryAdapter(jpaBlockRepository, jpaUserRepository, mapper);
    }

    @Bean
    public BlockUserUseCase blockUserUseCase(BlockRepository blockRepository,
                                             UserRepository userRepository,
                                             FollowRepository followRepository) {
        return new BlockUserService(blockRepository, userRepository, followRepository);
    }

    @Bean
    public UnblockUserUseCase unblockUserUseCase(BlockRepository blockRepository) {
        return new UnblockUserService(blockRepository);
    }

    @Bean
    public IsBlockedUseCase isBlockedUseCase(BlockRepository blockRepository) {
        return new IsBlockedService(blockRepository);
    }

    @Bean
    public IsBlockedEitherUseCase isBlockedEitherUseCase(BlockRepository blockRepository) {
        return new IsBlockedService(blockRepository);
    }

    @Bean
    public GetBlockedUserIdsUseCase getBlockedUserIdsUseCase(BlockRepository blockRepository) {
        return new GetBlockedUserIdsService(blockRepository);
    }

    @Bean
    public GetUsersWhoBlockedMeUseCase getUsersWhoBlockedMeUseCase(BlockRepository blockRepository) {
        return new GetBlockedUserIdsService(blockRepository);
    }
}
