package com.socialpulse.app.security.permission;

import static org.junit.jupiter.api.Assertions.*;

import java.util.Set;

import org.junit.jupiter.api.Test;

class RolePermissionsTest {

    @Test
    void guestHasOnlyPostRead() {
        Set<AppPermission> guest = RolePermissions.BY_ROLE.get("GUEST");
        assertEquals(1, guest.size());
        assertTrue(guest.contains(AppPermission.POST_READ));
    }

    @Test
    void userHasBasicPermissions() {
        Set<AppPermission> user = RolePermissions.BY_ROLE.get("USER");
        assertTrue(user.contains(AppPermission.POST_CREATE));
        assertTrue(user.contains(AppPermission.COMMENT_CREATE));
        assertTrue(user.contains(AppPermission.FOLLOW_CREATE));
        assertTrue(user.contains(AppPermission.CHAT_READ));
        assertFalse(user.contains(AppPermission.ADMIN_ACCESS));
        assertFalse(user.contains(AppPermission.POST_MANAGE));
    }

    @Test
    void adminHasAllUserPermissionsPlusAdmin() {
        Set<AppPermission> admin = RolePermissions.BY_ROLE.get("ADMIN");
        Set<AppPermission> user = RolePermissions.BY_ROLE.get("USER");

        assertTrue(admin.containsAll(user));
        assertTrue(admin.contains(AppPermission.ADMIN_ACCESS));
        assertTrue(admin.contains(AppPermission.POST_MANAGE));
        assertTrue(admin.contains(AppPermission.USER_MANAGE));
        assertTrue(admin.contains(AppPermission.REPORT_MANAGE));
    }

    @Test
    void allRolesExist() {
        assertTrue(RolePermissions.BY_ROLE.containsKey("GUEST"));
        assertTrue(RolePermissions.BY_ROLE.containsKey("USER"));
        assertTrue(RolePermissions.BY_ROLE.containsKey("ADMIN"));
    }
}
