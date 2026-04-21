package com.socialpulse.app.user.adapter.persistence;

import java.util.Optional;

import com.socialpulse.app.user.domain.repository.UserProfileRepository;
import com.socialpulse.app.user.domain.model.UserProfile;
import com.socialpulse.app.user.infrastructure.persistence.mapper.UserPersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaUserProfileRepository;

public class UserProfileRepositoryAdapter implements UserProfileRepository {
	private final JpaUserProfileRepository jpaUserProfileRepository;
	private final UserPersistenceMapper userPersistenceMapper;

	public UserProfileRepositoryAdapter(JpaUserProfileRepository jpaUserProfileRepository,
			UserPersistenceMapper userPersistenceMapper) {
		this.jpaUserProfileRepository = jpaUserProfileRepository;
		this.userPersistenceMapper = userPersistenceMapper;
	}

	@Override
	public Optional<UserProfile> findByUserId(Long userId) {
		return jpaUserProfileRepository.findByUserId(userId)
				.map(userPersistenceMapper::toDomain);
	}

	@Override
	public Optional<UserProfile> findByUsername(String username) {
		return jpaUserProfileRepository.findByUsername(username)
				.map(userPersistenceMapper::toDomain);
	}

}


