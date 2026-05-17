package com.socialpulse.app.ai.inference;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import com.socialpulse.app.ai.shared.LightGbmFeatureSchema;
import com.socialpulse.app.feed.application.dto.AuthorFeatures;
import com.socialpulse.app.feed.application.dto.InteractionFeatures;
import com.socialpulse.app.feed.application.dto.PostFeatures;
import com.socialpulse.app.feed.application.dto.RankingFeatures;

public class LightGbmFeatureVectorizer {
    public static final List<String> FEATURE_ORDER = LightGbmFeatureSchema.FEATURE_ORDER;
    public static final double DEFAULT_NUMERIC_VALUE = LightGbmFeatureSchema.DEFAULT_NUMERIC_VALUE;
    public static final double DEFAULT_UPVOTE_RATIO = LightGbmFeatureSchema.DEFAULT_UPVOTE_RATIO;
    public static final double DEFAULT_LAST_INTERACTION_HOURS = LightGbmFeatureSchema.DEFAULT_LAST_INTERACTION_HOURS;

    public Map<String, Double> toFeatureMap(RankingFeatures features) {
        PostFeatures postFeatures = features.getPostFeatures();
        AuthorFeatures authorFeatures = features.getAuthorFeatures();
        InteractionFeatures interactionFeatures = features.getInteractionFeatures();

        Map<String, Double> vector = new LinkedHashMap<>();
        vector.put("content_length", safeInt(postFeatures != null ? postFeatures.getContentLength() : null));
        vector.put("has_multimedia", toBinary(postFeatures != null ? postFeatures.getHasMultimedia() : null));
        vector.put("is_share_post", toBinary(postFeatures != null ? postFeatures.getIsSharePost() : null));
        vector.put("post_age_hours", safeDouble(postFeatures != null ? postFeatures.getPostAgeHours() : null));
        vector.put("hot_score", safeDouble(postFeatures != null ? postFeatures.getHotScore() : null));
        vector.put("upvote_ratio", safeDouble(postFeatures != null ? postFeatures.getUpvoteRatio() : null, DEFAULT_UPVOTE_RATIO));

        vector.put("author_seniority", safeDouble(authorFeatures != null ? authorFeatures.getSeniorityYears() : null));
        vector.put("author_post_count", safeLong(authorFeatures != null ? authorFeatures.getPostCount() : null));
        vector.put("author_engagement_rate", safeDouble(authorFeatures != null ? authorFeatures.getAveragePopularity() : null));

        vector.put("interaction_count_7d", safeLong(interactionFeatures != null ? interactionFeatures.getInteractionCount7d() : null));
        vector.put("interaction_count_30d", safeLong(interactionFeatures != null ? interactionFeatures.getInteractionCount30d() : null));
        vector.put("hours_since_last_interaction", safeDouble(
                interactionFeatures != null ? interactionFeatures.getHoursSinceLastInteraction() : null,
                DEFAULT_LAST_INTERACTION_HOURS));
        vector.put("affinity_score", safeDouble(interactionFeatures != null ? interactionFeatures.getAffinityScore() : null));

        double upvoteCount = safeLong(postFeatures != null ? postFeatures.getUpvoteCount() : null);
        double downvoteCount = safeLong(postFeatures != null ? postFeatures.getDownvoteCount() : null);
        double commentCount = safeLong(postFeatures != null ? postFeatures.getCommentCount() : null);
        double shareCount = safeLong(postFeatures != null ? postFeatures.getShareCount() : null);
        double viewCount = safeLong(postFeatures != null ? postFeatures.getViewCount() : null);

        vector.put("upvote_count", upvoteCount);
        vector.put("downvote_count", downvoteCount);
        vector.put("comment_count", commentCount);
        vector.put("share_count", shareCount);
        vector.put("view_count", viewCount);
        vector.put("popularity", safeDouble(postFeatures != null ? postFeatures.getPopularity() : null,
                upvoteCount + commentCount + shareCount));
        return vector;
    }

    public List<String> getFeatureOrder() {
        return FEATURE_ORDER;
    }

    private double toBinary(Boolean value) {
        return value != null && value ? 1.0 : 0.0;
    }

    private double safeDouble(Double value) {
        return safeDouble(value, DEFAULT_NUMERIC_VALUE);
    }

    private double safeDouble(Double value, double defaultValue) {
        return value != null ? value : defaultValue;
    }

    private double safeLong(Long value) {
        return value != null ? value.doubleValue() : DEFAULT_NUMERIC_VALUE;
    }

    private double safeInt(Integer value) {
        return value != null ? value.doubleValue() : DEFAULT_NUMERIC_VALUE;
    }
}
