package com.socialpulse.app.security.permission;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum AppPermission {

    // Post
    POST_READ("post:read", "Read posts"),
    POST_CREATE("post:create", "Create posts"),
    POST_UPDATE("post:update", "Update own posts"),
    POST_DELETE("post:delete", "Delete own posts"),
    POST_REACT("post:react", "React to posts"),
    POST_MANAGE("post:manage", "Manage any post"),

    // Comment
    COMMENT_READ("comment:read", "Read comments"),
    COMMENT_CREATE("comment:create", "Create comments"),
    COMMENT_UPDATE("comment:update", "Update own comments"),
    COMMENT_DELETE("comment:delete", "Delete own comments"),
    COMMENT_REACT("comment:react", "React to comments"),
    COMMENT_MANAGE("comment:manage", "Manage any comment"),

    // User
    USER_CREATE("user:create", "Create own profile"),
    USER_READ("user:read", "View user profiles"),
    USER_UPDATE("user:update", "Update own profile"),
    USER_DELETE("user:delete", "Delete own account"),
    USER_MANAGE("user:manage", "Manage all users"),
    USER_MODERATE("user:moderate", "Moderate users"),

    // Follow
    FOLLOW_READ("follow:read", "Read follow graph"),
    FOLLOW_CREATE("follow:create", "Follow users"),
    FOLLOW_DELETE("follow:delete", "Unfollow users"),

    // Feed
    FEED_READ("feed:read", "Read personalized feed"),

    // Report
    REPORT_CREATE("report:create", "Create reports"),
    REPORT_MANAGE("report:manage", "Manage reports and moderation queue"),

    // Discovery
    DISCOVERY_READ("discovery:read", "Read discovery and search results"),
    DISCOVERY_WRITE("discovery:write", "Write and save search history"),
    DISCOVERY_DELETE("discovery:delete", "Delete search history"),

    // Bookmark
    BOOKMARK_CREATE("bookmark:create", "Create bookmarks"),
    BOOKMARK_DELETE("bookmark:delete", "Delete bookmarks"),
    BOOKMARK_READ("bookmark:read", "Read bookmarks"),

    // Notification
    NOTIFICATION_READ("notification:read", "Read notifications"),
    NOTIFICATION_UPDATE("notification:update", "Mark notifications as read"),

    // Chat
    CHAT_READ("chat:read", "Read chat conversations and messages"),
    CHAT_CREATE("chat:create", "Create conversations and send messages"),

    // Topic & Admin
    TOPIC_MANAGE("topic:manage", "Create, update, delete topics"),
    ADMIN_ACCESS("admin:access", "Access admin endpoints");

    private final String value;
    private final String description;

    /** Convenience for {@code @PreAuthorize("hasAuthority(T(...).POST_READ.value)")} */
    @Override
    public String toString() {
        return value;
    }
}
