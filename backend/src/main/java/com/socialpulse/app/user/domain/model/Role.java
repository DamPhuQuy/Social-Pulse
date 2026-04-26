package com.socialpulse.app.user.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {
    private Long id;
    private String name;
    private String description;
    @Builder.Default
    private Set<Permission> permissions = new HashSet<>();
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public boolean hasPermission(String permission) {
        return permissions.stream().anyMatch(p -> p.getName().equals(permission));
    }
}
