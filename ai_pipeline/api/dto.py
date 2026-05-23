from __future__ import annotations

from pydantic import AliasChoices, BaseModel, ConfigDict, Field

class ApiDto(BaseModel):
    model_config = ConfigDict(populate_by_name=True, extra="ignore")

class PostFeaturesDto(ApiDto):

    content_length: int | None = Field(
        default=None,
        validation_alias=AliasChoices("content_length", "contentLength"),
        serialization_alias="content_length"
    )
    has_multimedia: bool | None = Field(
        default=None,
        validation_alias=AliasChoices("has_multimedia", "hasMultimedia"),
        serialization_alias="has_multimedia"
    )
    is_share_post: bool | None = Field(
        default=None,
        validation_alias=AliasChoices("is_share_post", "isSharePost"),
        serialization_alias="is_share_post"
    )
    post_age_hours: float | None = Field(
        default=None,
        validation_alias=AliasChoices("post_age_hours", "postAgeHours"),
        serialization_alias="post_age_hours"
    )
    hot_score: float | None = Field(
        default=None,
        validation_alias=AliasChoices("hot_score", "hotScore"),
        serialization_alias="hot_score"
    )
    upvote_ratio: float | None = Field(
        default=None,
        validation_alias=AliasChoices("upvote_ratio", "upvoteRatio"),
        serialization_alias="upvote_ratio"
    )
    upvote_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("upvote_count", "upvoteCount"),
        serialization_alias="upvote_count"
    )
    downvote_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("downvote_count", "downvoteCount"),
        serialization_alias="downvote_count"
    )
    comment_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("comment_count", "commentCount"),
        serialization_alias="comment_count"
    )
    share_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("share_count", "shareCount"),
        serialization_alias="share_count"
    )
    view_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("view_count", "viewCount"),
        serialization_alias="view_count"
    )


class AuthorFeaturesDto(ApiDto):

    seniority_years: float | None = Field(
        default=None,
        validation_alias=AliasChoices("seniority_years", "seniorityYears"),
        serialization_alias="seniority_years"
    )
    post_count: int | None = Field(
        default=None,
        validation_alias=AliasChoices("post_count", "postCount"),
        serialization_alias="post_count"
    )
    average_popularity: float | None = Field(
        default=None,
        validation_alias=AliasChoices("average_popularity", "averagePopularity"),
        serialization_alias="average_popularity"
    )


class InteractionFeaturesDto(ApiDto):

    interaction_count_7d: int | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_count_7d", "interaction_count7d", "interactionCount7d"),
        serialization_alias="interaction_count_7d"
    )
    interaction_count_30d: int | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_count_30d", "interaction_count30d", "interactionCount30d"),
        serialization_alias="interaction_count_30d"
    )
    hours_since_last_interaction: float | None = Field(
        default=None,
        validation_alias=AliasChoices("hours_since_last_interaction", "hoursSinceLastInteraction"),
        serialization_alias="hours_since_last_interaction"
    )
    affinity_score: float | None = Field(
        default=None,
        validation_alias=AliasChoices("affinity_score", "affinityScore"),
        serialization_alias="affinity_score"
    )


class RankingFeaturesDto(ApiDto):

    post_id: int = Field(validation_alias=AliasChoices("post_id", "postId"), serialization_alias="post_id")
    post_features: PostFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("post_features", "postFeatures"),
        serialization_alias="post_features"
    )
    author_features: AuthorFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("author_features", "authorFeatures"),
        serialization_alias="author_features"
    )
    interaction_features: InteractionFeaturesDto | None = Field(
        default=None,
        validation_alias=AliasChoices("interaction_features", "interactionFeatures"),
        serialization_alias="interaction_features"
    )


class RankingRequestDto(ApiDto):
    feature_schema_version: str = Field(
        default="v1",
        validation_alias=AliasChoices("feature_schema_version", "featureSchemaVersion"),
        serialization_alias="feature_schema_version",
    )
    features: list[RankingFeaturesDto]


class RankingResponseDto(ApiDto):
    post_id: int = Field(serialization_alias="post_id")
    score: float
    feature_schema_version: str = Field(serialization_alias="feature_schema_version")
