"""Feature schema constants - source of truth for feature order and defaults."""


class RankingFeatureSchema:
    DEFAULT_SCHEMA_VERSION = "v1"
    DEFAULT_NUMERIC_VALUE = 0.0
    DEFAULT_UPVOTE_RATIO = 0.5
    DEFAULT_LAST_INTERACTION_HOURS = 999.0
    DEFAULT_CAP_PERCENTILE = 99.0

    # NOTE: upvote_count, downvote_count, comment_count, share_count, view_count are intentionally
    # excluded from training. Pushshift provides these as final-snapshot values (at crawl time),
    # which are monotonically correlated with the label (log1p popularity). Including them would
    # cause the model to memorise rather than generalise. They remain in the inference vectorizer
    # since serving-time values reflect real current state.
    FEATURE_ORDER: list[str] = [
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
    ]

    LOG_TRANSFORM_FEATURES: tuple[str, ...] = (
        "interaction_count_7d",
        "interaction_count_30d",
    )

    CAP_FEATURES: tuple[str, ...] = (
        "content_length",
        "post_age_hours",
        "hot_score",
        "author_seniority",
        "author_post_count",
        "author_engagement_rate",
        "hours_since_last_interaction",
    )
