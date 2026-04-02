package com.socialpulse.app.user.entity;

import java.util.Set;

public enum UserRole {
    GUEST(Set.of(
            Permission.READ_POSTS
    )),

    USER(Set.of(
            Permission.READ_POSTS,
            Permission.CREATE_POSTS,
            Permission.DELETE_POSTS
    )),

    ADMIN(Set.of(
            Permission.MODERATE_USERS,
            Permission.MANAGE_USERS
    ));

    private final Set<Permission> permissions;

    UserRole(Set<Permission> permissions) {
        this.permissions = permissions;
    }

    public Set<Permission> getPermissions() {
        return permissions;
    }
}
