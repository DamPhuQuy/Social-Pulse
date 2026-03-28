package com.socialpulse.app.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.user.entity.User;
import com.socialpulse.app.user.entity.UserProfile;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u.profile from User u where u.id = :userId")
    Optional<UserProfile> findProfileById(@Param("userId") Long userId);

    @Query("select u.profile from User u where u.username = :username")
    Optional<UserProfile> findProfileByUsername(@Param("username") String username);
}

