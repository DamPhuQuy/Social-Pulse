package com.socialpulse.app.user.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.auth.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.adapter.out.UserProfileRepositoryAdapter;
import com.socialpulse.app.user.adapter.out.UserRepositoryAdapter;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.dto.mapper.UserProfileMapper;
import com.socialpulse.app.user.application.port.in.CreateUserUseCase;
import com.socialpulse.app.user.application.port.in.GetUserProfileUseCase;
import com.socialpulse.app.user.application.port.out.UserProfileRepositoryPort;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;
import com.socialpulse.app.user.application.service.CreateUserService;
import com.socialpulse.app.user.application.service.GetUserProfileService;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserEntityToDomainMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserProfileRepository;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Configuration
public class UserConfig {
    @Bean
    public UserRepositoryPort userRepositoryPort(
            JpaUserRepository jpaUserRepository,
            UserEntityToDomainMapper userEntityToDomainMapper) {
        return new UserRepositoryAdapter(jpaUserRepository, userEntityToDomainMapper);
    }

    @Bean
    public UserProfileRepositoryPort userProfileRepositoryPort(
            JpaUserProfileRepository jpaUserProfileRepository,
            UserEntityToDomainMapper userEntityToDomainMapper) {
        return new UserProfileRepositoryAdapter(jpaUserProfileRepository, userEntityToDomainMapper);
    }

    @Bean
    public GetUserProfileUseCase getUserProfileUseCase(
            UserProfileRepositoryPort userProfileRepositoryPort,
            UserProfileMapper userProfileMapper) {
        return new GetUserProfileService(userProfileRepositoryPort, userProfileMapper);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserRepositoryPort userRepository,
            AppPasswordEncoder appPasswordEncoder,
            UserMapper userMapper
    ) {
        return new CreateUserService(userRepository, appPasswordEncoder, userMapper);
    }
}
