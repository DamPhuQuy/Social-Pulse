package com.socialpulse.app.follow.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.follow.adapter.persistence.FollowRepositoryAdapter;
import com.socialpulse.app.follow.application.dto.mapper.FollowMapper;
import com.socialpulse.app.follow.application.service.FollowUserService;
import com.socialpulse.app.follow.application.service.GetFollowersService;
import com.socialpulse.app.follow.application.service.GetFollowingService;
import com.socialpulse.app.follow.application.service.UnfollowUserService;
import com.socialpulse.app.follow.application.usecase.FollowUserUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowingUseCase;
import com.socialpulse.app.follow.application.usecase.UnfollowUserUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.follow.infrastructure.persistence.mapper.FollowPersistenceMapper;
import com.socialpulse.app.follow.infrastructure.persistence.repository.JpaFollowRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Configuration
public class FollowConfig {

    @Bean
    public FollowRepository followRepository(JpaFollowRepository jpaFollowRepository,
                                             JpaUserRepository jpaUserRepository,
                                             FollowPersistenceMapper mapper) {
        return new FollowRepositoryAdapter(jpaFollowRepository, jpaUserRepository, mapper);
    }

    @Bean
    public FollowUserUseCase followUserUseCase(FollowRepository followRepository,
                                               UserRepository userRepository,
                                               FollowMapper followMapper) {
        return new FollowUserService(followRepository, userRepository, followMapper);
    }

    @Bean
    public UnfollowUserUseCase unfollowUserUseCase(FollowRepository followRepository) {
        return new UnfollowUserService(followRepository);
    }

    @Bean
    public GetFollowersUseCase getFollowersUseCase(FollowRepository followRepository,
                                                   UserRepository userRepository) {
        return new GetFollowersService(followRepository, userRepository);
    }

    @Bean
    public GetFollowingUseCase getFollowingUseCase(FollowRepository followRepository,
                                                   UserRepository userRepository) {
        return new GetFollowingService(followRepository, userRepository);
    }
}
