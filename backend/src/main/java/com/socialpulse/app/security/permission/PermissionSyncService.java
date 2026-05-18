package com.socialpulse.app.security.permission;

import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.socialpulse.app.user.infrastructure.persistence.entity.PermissionEntity;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaPermissionRepository;
import com.socialpulse.app.user.infrastructure.persistence.repository.JpaRoleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

/**
 * Syncs {@link AppPermission} enum values into the DB at startup.
 * Idempotent: safe to run on every boot.
 * Runs after Flyway (schema must already exist).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PermissionSyncService implements ApplicationRunner {

    private final JpaPermissionRepository permissionRepo;
    private final JpaRoleRepository roleRepo;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        Map<String, PermissionEntity> allPermissions = syncPermissions();
        syncRolePermissions(allPermissions);
    }

    /** Upserts every enum value into the permissions table. */
    private Map<String, PermissionEntity> syncPermissions() {
        Map<String, PermissionEntity> existing = permissionRepo.findAll().stream()
                .collect(Collectors.toMap(PermissionEntity::getName, Function.identity()));

        for (AppPermission p : AppPermission.values()) {
            existing.computeIfAbsent(p.getValue(), name -> {
                log.info("Registering new permission: {}", name);
                return permissionRepo.save(
                        PermissionEntity.builder()
                                .name(p.getValue())
                                .description(p.getDescription())
                                .build()
                );
            });
        }
        return existing;
    }

    /** Ensures each role has exactly the permissions declared in {@link RolePermissions}. */
    private void syncRolePermissions(Map<String, PermissionEntity> allPermissions) {
        for (Map.Entry<String, Set<AppPermission>> entry : RolePermissions.BY_ROLE.entrySet()) {
            String roleName = entry.getKey();
            Set<String> expected = entry.getValue().stream()
                    .map(AppPermission::getValue)
                    .collect(Collectors.toSet());

            roleRepo.findByName(roleName).ifPresent(role -> {
                Set<String> current = role.getPermissions().stream()
                        .map(PermissionEntity::getName)
                        .collect(Collectors.toSet());

                // Add missing
                expected.stream()
                        .filter(p -> !current.contains(p))
                        .forEach(p -> {
                            log.info("Granting '{}' to role '{}'", p, roleName);
                            role.getPermissions().add(allPermissions.get(p));
                        });

                // Remove revoked (permissions removed from the enum mapping)
                role.getPermissions().removeIf(p -> {
                    boolean revoked = !expected.contains(p.getName());
                    if (revoked) log.info("Revoking '{}' from role '{}'", p.getName(), roleName);
                    return revoked;
                });

                roleRepo.save(role);
            });
        }
    }
}
