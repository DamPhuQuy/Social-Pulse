"""Feature schema constants - source of truth for feature order and defaults."""


class RankingFeatureSchema:
    DEFAULT_SCHEMA_VERSION = "v2"
    DEFAULT_NUMERIC_VALUE = 0.0
    DEFAULT_LAST_INTERACTION_HOURS = 999.0
    DEFAULT_CAP_PERCENTILE = 99.0

    # v2 excludes all target-derived Reddit snapshot signals. Engagement values
    # known only after crawl time would leak the label into training.
    FEATURE_ORDER: list[str] = [
        "content_length",
        "has_multimedia",
        "is_share_post",
        "post_age_hours",
        "author_seniority",
        "author_post_count",
        "author_engagement_rate",
        "interaction_count_7d",
        "interaction_count_30d",
        "hours_since_last_interaction",
        "affinity_score",
    ]

    LOG_TRANSFORM_FEATURES: tuple[str, ...] = (
        "interaction_count_7d",
        "interaction_count_30d",
    )

    CAP_FEATURES: tuple[str, ...] = (
        "content_length",
        "post_age_hours",
        "author_seniority",
        "author_post_count",
        "author_engagement_rate",
        "hours_since_last_interaction",
    )
