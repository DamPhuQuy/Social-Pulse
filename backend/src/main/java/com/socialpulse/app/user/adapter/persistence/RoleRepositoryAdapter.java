package com.socialpulse.app.user.adapter.persistence;

import java.util.Optional;

import org.springframework.stereotype.Component;

import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.domain.repository.RoleRepository;
import com.socialpulse.app.user.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaRoleRepository;

@Component
public class RoleRepositoryAdapter implements RoleRepository {

    private final JpaRoleRepository jpaRoleRepository;
    private final RolePersistenceMapper rolePersistenceMapper;

    public RoleRepositoryAdapter(JpaRoleRepository jpaRoleRepository, RolePersistenceMapper rolePersistenceMapper) {
        this.jpaRoleRepository = jpaRoleRepository;
        this.rolePersistenceMapper = rolePersistenceMapper;
    }

    @Override
    public Optional<Role> findByName(String name) {
        return jpaRoleRepository.findByName(name)
                .map(rolePersistenceMapper::toDomain);
    }

    @Override
    public Role save(Role role) {
        return rolePersistenceMapper.toDomain(
                jpaRoleRepository.save(rolePersistenceMapper.toEntity(role))
        );
    }
}
