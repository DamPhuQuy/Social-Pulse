package com.socialpulse.app.user.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByUsername(String username);

	Optional<UserEntity> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	List<UserEntity> findAllByIdIn(Set<Long> ids);
}

