package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

/**
 * Flat feature snapshot for a single (viewer, post) pair.
 * Used for both serving (AI request) and training (CSV/parquet export).
 *
 * <p>INVARIANT: Features must be captured BEFORE user interaction
 * (at impression time) to prevent data leakage.</p>
 *
 * <p>All fields are primitives — no nested DTOs — for easy
 * serialization, CSV export, parquet mapping, and feature versioning.</p>
 */
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeatureSnapshot {
    // === IDs ===
    private Long viewerId;
    private Long postId;
    private Long authorId;

    // === Post Features ===
    private double hotScore;
    private double upvoteRatio;
    private boolean hasImage;
    private int contentLength;
    private double postAgeHours;
    private long upvoteCount;
    private long downvoteCount;
    private long cmtCount;
    private long shareCount;
    private long viewCount;

    // === Interaction Features ===
    private int interactionCount7d;
    private int interactionCount30d;
    private double affinityScore;
    private double lastInteractionHours;

    // === Label (training only) ===
    private Boolean clicked;

    // === Metadata ===
    private LocalDateTime snapshotTime;
}
