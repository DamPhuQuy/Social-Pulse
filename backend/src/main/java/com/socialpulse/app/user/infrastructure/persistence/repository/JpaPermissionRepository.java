package com.socialpulse.app.user.infrastructure.persistence.repository;

import com.socialpulse.app.user.infrastructure.persistence.entity.PermissionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface JpaPermissionRepository extends JpaRepository<PermissionEntity, Long> {
    Optional<PermissionEntity> findByName(String name);
}
