package com.socialpulse.app.user.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.user.entity.User;


@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);

    Optional<User> findByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByEmail(String email);

    @Query("select u from User u join fetch u.profile where u.id = :userId")
    Optional<User> findProfileById(@Param("userId") Long userId);

    @Query("select u from User u join fetch u.profile where u.username = :username")
    Optional<User> findProfileByUsername(@Param("username") String username);

    @Query("select u from User u join fetch u.profile where u.email = :email")
    Optional<User> findProfileByEmail(@Param("email") String email);
}

