package com.socialpulse.app.ai.lightgbm;

import java.util.List;

public final class LightGbmFeatureSchema {
    public static final String DEFAULT_SCHEMA_VERSION = "v1";

    public static final double DEFAULT_NUMERIC_VALUE = 0.0;
    public static final double DEFAULT_UPVOTE_RATIO = 0.5;
    public static final double DEFAULT_LAST_INTERACTION_HOURS = 999.0;

    public static final List<String> FEATURE_ORDER = List.of(
            "content_length",
            "has_multimedia",
            "is_share_post",
            "post_age_hours",
            "hot_score",
            "upvote_ratio",
            "author_seniority",
            "author_post_count",
            "author_engagement_rate",
            "interaction_count_7d",
            "interaction_count_30d",
            "hours_since_last_interaction",
            "affinity_score",
            "upvote_count",
            "downvote_count",
            "comment_count",
            "share_count",
            "view_count",
            "popularity");

    private LightGbmFeatureSchema() {
    }
}
