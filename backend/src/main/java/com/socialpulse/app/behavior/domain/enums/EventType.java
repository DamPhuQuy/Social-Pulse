package com.socialpulse.app.behavior.domain.enums;

public enum EventType {
    // exposure
    IMPRESSION,

    // Engagement
    CLICK,
    UPVOTE,
    DOWNVOTE,
    COMMENT,
    SHARE,

    // Behavioral
    DWELL,
    SKIP, // skip = impression + dwell < threshold

    // Negative
    HIDE,
    REPORT,
    UNFOLLOW,

    // Socialize
    FOLLOW,
    VIEW_PROFILE
}
