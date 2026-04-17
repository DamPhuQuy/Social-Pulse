package com.socialpulse.app.user.enums;

import java.util.Set;

public enum UserRole {
    GUEST(Set.of(
            Permission.READ_POSTS
    )),

    USER(Set.of(
            Permission.VIEW_PROFILE,
            Permission.VIEW_OTHER_PROFILE,
            Permission.READ_POSTS,
            Permission.CREATE_POST,
            Permission.DELETE_POST,
            Permission.CREATE_COMMENT,
            Permission.DELETE_COMMENT
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
