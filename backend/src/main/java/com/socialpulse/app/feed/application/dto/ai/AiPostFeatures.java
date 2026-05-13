package com.socialpulse.app.feed.application.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiPostFeatures {
    private Long postId;
    private Long authorId;
    private String createdAt;
    private Integer contentLength;
    private Boolean hasImage;
    private Long upvoteCount;
    private Long downvoteCount;
    private Long commentCount;
    private Long shareCount;
    private Long viewCount;
    private Double hotScore;
    private Double upvoteRatio;
    private Boolean isSharePost;
    private Double postAgeHours;
}

