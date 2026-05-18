package com.socialpulse.app.security.permission;

import static com.socialpulse.app.security.permission.AppPermission.ADMIN_ACCESS;
import static com.socialpulse.app.security.permission.AppPermission.BOOKMARK_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.BOOKMARK_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.BOOKMARK_READ;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_MANAGE;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_REACT;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_READ;
import static com.socialpulse.app.security.permission.AppPermission.COMMENT_UPDATE;
import static com.socialpulse.app.security.permission.AppPermission.DISCOVERY_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.DISCOVERY_READ;
import static com.socialpulse.app.security.permission.AppPermission.DISCOVERY_WRITE;
import static com.socialpulse.app.security.permission.AppPermission.FEED_READ;
import static com.socialpulse.app.security.permission.AppPermission.FOLLOW_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.FOLLOW_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.FOLLOW_READ;
import static com.socialpulse.app.security.permission.AppPermission.NOTIFICATION_READ;
import static com.socialpulse.app.security.permission.AppPermission.NOTIFICATION_UPDATE;
import static com.socialpulse.app.security.permission.AppPermission.POST_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.POST_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.POST_MANAGE;
import static com.socialpulse.app.security.permission.AppPermission.POST_REACT;
import static com.socialpulse.app.security.permission.AppPermission.POST_READ;
import static com.socialpulse.app.security.permission.AppPermission.POST_UPDATE;
import static com.socialpulse.app.security.permission.AppPermission.REPORT_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.REPORT_MANAGE;
import static com.socialpulse.app.security.permission.AppPermission.TOPIC_MANAGE;
import static com.socialpulse.app.security.permission.AppPermission.USER_CREATE;
import static com.socialpulse.app.security.permission.AppPermission.USER_DELETE;
import static com.socialpulse.app.security.permission.AppPermission.USER_MANAGE;
import static com.socialpulse.app.security.permission.AppPermission.USER_MODERATE;
import static com.socialpulse.app.security.permission.AppPermission.USER_READ;
import static com.socialpulse.app.security.permission.AppPermission.USER_UPDATE;

import java.util.EnumSet;
import java.util.Map;
import java.util.Set;

/**
 * Single source of truth for which permissions each role holds.
 * To grant a new permission to a role, add it here — no SQL needed.
 */
public final class RolePermissions {

    private RolePermissions() {}

    protected static final Set<AppPermission> GUEST = EnumSet.of(
            POST_READ
    );

    protected static final Set<AppPermission> USER = EnumSet.of(
            POST_READ, POST_CREATE, POST_UPDATE, POST_DELETE, POST_REACT,
            COMMENT_READ, COMMENT_CREATE, COMMENT_UPDATE, COMMENT_DELETE, COMMENT_REACT,
            USER_CREATE, USER_READ, USER_UPDATE, USER_DELETE,
            FOLLOW_READ, FOLLOW_CREATE, FOLLOW_DELETE,
            FEED_READ,
            REPORT_CREATE,
            DISCOVERY_READ, DISCOVERY_WRITE, DISCOVERY_DELETE,
            BOOKMARK_CREATE, BOOKMARK_DELETE, BOOKMARK_READ,
            NOTIFICATION_READ, NOTIFICATION_UPDATE
    );

    protected static final Set<AppPermission> ADMIN = EnumSet.copyOf(USER);

    static {
        // ADMIN gets everything USER has, plus admin-only permissions
        ((EnumSet<AppPermission>) ADMIN).addAll(EnumSet.of(
                POST_MANAGE,
                COMMENT_MANAGE,
                USER_MANAGE, USER_MODERATE,
                REPORT_MANAGE,
                TOPIC_MANAGE,
                ADMIN_ACCESS
        ));
    }

    public static final Map<String, Set<AppPermission>> BY_ROLE = Map.of(
            "GUEST", GUEST,
            "USER",  USER,
            "ADMIN", ADMIN
    );
}
