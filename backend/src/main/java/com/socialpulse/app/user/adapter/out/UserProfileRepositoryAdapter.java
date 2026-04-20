package com.socialpulse.app.user.adapter.out;

import java.util.Optional;

import com.socialpulse.app.user.application.port.out.UserProfileRepositoryPort;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserEntityToDomain;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserProfileRepository;

public class UserProfileRepositoryAdapter implements UserProfileRepositoryPort {
	private final JpaUserProfileRepository jpaUserProfileRepository;
	private final UserEntityToDomain userEntityToDomainMapper;

	public UserProfileRepositoryAdapter(JpaUserProfileRepository jpaUserProfileRepository,
			UserEntityToDomain userEntityToDomainMapper) {
		this.jpaUserProfileRepository = jpaUserProfileRepository;
		this.userEntityToDomainMapper = userEntityToDomainMapper;
	}

	@Override
	public Optional<UserProfile> findByUserId(Long userId) {
		return jpaUserProfileRepository.findByUserId(userId)
				.map(userEntityToDomainMapper::toDomain);
	}

	@Override
	public Optional<UserProfile> findByUsername(String username) {
		return jpaUserProfileRepository.findByUsername(username)
				.map(userEntityToDomainMapper::toDomain);
	}

}
