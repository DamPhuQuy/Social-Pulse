package com.socialpulse.app.user.application.service;

import com.socialpulse.app.user.domain.model.Role;
import com.socialpulse.app.user.domain.repository.RoleRepository;
import com.socialpulse.app.user.domain.model.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Set;

@Service
public class UserRoleService {

    private final RoleRepository roleRepository;

    public UserRoleService(RoleRepository roleRepository) {
        this.roleRepository = roleRepository;
    }

    @Transactional
    public void assignDefaultRole(User user) {
        Role userRole = roleRepository.findByName("USER")
                .orElseThrow(() -> new IllegalStateException("Default USER role not found in database"));

        user.getRoles().add(userRole);
    }

    @Transactional
    public void assignRoles(User user, Set<String> roleNames) {
        user.getRoles().clear();

        roleNames.forEach(roleName -> {
            Role role = roleRepository.findByName(roleName)
                    .orElseThrow(() -> new IllegalArgumentException("Role not found: " + roleName));
            user.getRoles().add(role);
        });
    }
}
