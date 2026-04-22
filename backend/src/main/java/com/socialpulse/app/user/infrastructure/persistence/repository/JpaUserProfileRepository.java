package com.socialpulse.app.user.infrastructure.persistence.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.user.infrastructure.persistence.entity.UserProfileEntity;

@Repository
public interface JpaUserProfileRepository extends JpaRepository<UserProfileEntity, Long> {

    @Query("select p from UserProfileEntity p left join fetch p.user where p.id = :userId")
    Optional<UserProfileEntity> findByUserId(@Param("userId") Long userId);

    @Query("select p from UserProfileEntity p join fetch p.user u where u.username = :username")
    Optional<UserProfileEntity> findByUsername(@Param("username") String username);
}
