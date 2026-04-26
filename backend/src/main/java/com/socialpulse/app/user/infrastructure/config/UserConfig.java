package com.socialpulse.app.user.infrastructure.config;

import com.socialpulse.app.user.adapter.persistence.RoleRepositoryAdapter;
import com.socialpulse.app.user.application.service.UserRoleService;
import com.socialpulse.app.user.domain.repository.RoleRepository;
import com.socialpulse.app.user.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaRoleRepository;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.security.encoder.AppPasswordEncoder;
import com.socialpulse.app.user.adapter.persistence.UserProfileRepositoryAdapter;
import com.socialpulse.app.user.adapter.persistence.UserRepositoryAdapter;
import com.socialpulse.app.user.application.dto.mapper.UserMapper;
import com.socialpulse.app.user.application.usecase.CreateUserUseCase;
import com.socialpulse.app.user.application.usecase.GetUserProfileUseCase;
import com.socialpulse.app.user.domain.repository.UserProfileRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.application.service.CreateUserService;
import com.socialpulse.app.user.application.service.GetUserProfileService;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserProfileRepository;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserRepository;

@Configuration
public class UserConfig {

    // adapters --------------------------------------

    @Bean
    public UserRepository userRepositoryPort(
            JpaUserRepository jpaUserRepository,
            UserPersistenceMapper userPersistenceMapper) {
        return new UserRepositoryAdapter(jpaUserRepository, userPersistenceMapper);
    }

    @Bean
    public UserProfileRepository userProfileRepositoryPort(
            JpaUserProfileRepository jpaUserProfileRepository,
            UserPersistenceMapper userPersistenceMapper) {
        return new UserProfileRepositoryAdapter(jpaUserProfileRepository, userPersistenceMapper);
    }

    @Bean
    public RoleRepository roleRepository(
            JpaRoleRepository jpaRoleRepository,
            RolePersistenceMapper rolePersistenceMapper) {
        return new RoleRepositoryAdapter(jpaRoleRepository, rolePersistenceMapper);
    }

    // use cases --------------------------------------

    @Bean
    public GetUserProfileUseCase getUserProfileUseCase(
            UserProfileRepository userProfileRepositoryPort,
            UserMapper userMapper) {
        return new GetUserProfileService(userProfileRepositoryPort, userMapper);
    }

    @Bean
    public CreateUserUseCase createUserUseCase(
            UserRepository userRepository,
            AppPasswordEncoder appPasswordEncoder,
            UserMapper userMapper,
            UserRoleService userRoleService
    ) {
        return new CreateUserService(userRepository, appPasswordEncoder, userMapper, userRoleService);
    }
}


