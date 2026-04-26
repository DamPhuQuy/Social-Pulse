package com.socialpulse.app.security.adapter.persistence;

import com.socialpulse.app.security.domain.model.Role;
import com.socialpulse.app.security.domain.repository.RoleRepository;
import com.socialpulse.app.security.infrastructure.persistence.mapper.RolePersistenceMapper;
import com.socialpulse.app.security.infrastructure.persistence.repository.JpaRoleRepository;
import org.springframework.stereotype.Component;

import java.util.Optional;

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
