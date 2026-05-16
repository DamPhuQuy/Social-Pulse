package com.socialpulse.app.follow.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.follow.adapter.persistence.FollowRepositoryAdapter;
import com.socialpulse.app.follow.application.dto.mapper.FollowMapper;
import com.socialpulse.app.follow.application.service.FollowUserService;
import com.socialpulse.app.follow.application.service.FollowGraphPageService;
import com.socialpulse.app.follow.application.service.GetFollowCountsService;
import com.socialpulse.app.follow.application.service.GetFollowersService;
import com.socialpulse.app.follow.application.service.GetFollowStatusService;
import com.socialpulse.app.follow.application.service.GetFollowingService;
import com.socialpulse.app.follow.application.service.UnfollowUserService;
import com.socialpulse.app.follow.application.usecase.FollowUserUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowCountsUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowersUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowStatusUseCase;
import com.socialpulse.app.follow.application.usecase.GetFollowingUseCase;
import com.socialpulse.app.follow.application.usecase.UnfollowUserUseCase;
import com.socialpulse.app.follow.domain.repository.FollowRepository;
import com.socialpulse.app.follow.infrastructure.persistence.mapper.FollowPersistenceMapper;
import com.socialpulse.app.follow.infrastructure.persistence.repository.JpaFollowRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
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
                                               FollowMapper followMapper,
                                               NotificationCommandService notificationCommandService) {
        return new FollowUserService(followRepository, userRepository, followMapper, notificationCommandService);
    }

    @Bean
    public UnfollowUserUseCase unfollowUserUseCase(FollowRepository followRepository) {
        return new UnfollowUserService(followRepository);
    }

    @Bean
<<<<<<< HEAD
    public GetFollowersUseCase getFollowersUseCase(FollowRepository followRepository,
                                                   UserRepository userRepository) {
        return new GetFollowersService(followRepository, userRepository);
    }

    @Bean
    public GetFollowingUseCase getFollowingUseCase(FollowRepository followRepository,
                                                   UserRepository userRepository) {
        return new GetFollowingService(followRepository, userRepository);
=======
    public FollowGraphPageService followGraphPageService(UserRepository userRepository, UserMapper userMapper) {
        return new FollowGraphPageService(userRepository, userMapper);
    }

    @Bean
    public GetFollowersUseCase getFollowersUseCase(
            FollowRepository followRepository,
            UserRepository userRepository,
            FollowGraphPageService followGraphPageService) {
        return new GetFollowersService(followRepository, userRepository, followGraphPageService);
    }

    @Bean
    public GetFollowingUseCase getFollowingUseCase(
            FollowRepository followRepository,
            UserRepository userRepository,
            FollowGraphPageService followGraphPageService) {
        return new GetFollowingService(followRepository, userRepository, followGraphPageService);
    }

    @Bean
    public GetFollowStatusUseCase getFollowStatusUseCase(
            FollowRepository followRepository,
            UserRepository userRepository) {
        return new GetFollowStatusService(followRepository, userRepository);
    }

    @Bean
    public GetFollowCountsUseCase getFollowCountsUseCase(
            FollowRepository followRepository,
            UserRepository userRepository) {
        return new GetFollowCountsService(followRepository, userRepository);
>>>>>>> 607b960041ce7b4c689004bb05e77939ef44d3f2
    }
}
