package com.socialpulse.app.user.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

@Repository
public interface JpaUserRepository extends JpaRepository<UserEntity, Long> {
	Optional<UserEntity> findByUsername(String username);

	Optional<UserEntity> findByEmail(String email);

	boolean existsByUsername(String username);

	boolean existsByEmail(String email);

	List<UserEntity> findAllByIdIn(Set<Long> ids);

	@Query("""
			SELECT u
			FROM UserEntity u
			LEFT JOIN u.profile p
			WHERE LOWER(u.username) LIKE LOWER(CONCAT('%', :query, '%'))
			   OR LOWER(COALESCE(p.displayName, '')) LIKE LOWER(CONCAT('%', :query, '%'))
			ORDER BY u.username ASC
			""")
	Page<UserEntity> searchByQuery(@Param("query") String query, Pageable pageable);
}
