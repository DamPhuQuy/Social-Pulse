package com.socialpulse.app.feed.application.dto;

import com.fasterxml.jackson.databind.PropertyNamingStrategies;
import com.fasterxml.jackson.databind.annotation.JsonNaming;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonNaming(PropertyNamingStrategies.SnakeCaseStrategy.class)
public class PostFeatures {
    private Long postId;
    private Integer contentLength;
    private Boolean hasMultimedia;
    private Boolean isSharePost;
    private Double postAgeHours;
    private Double hotScore;
    private Double upvoteRatio;
    private Long upvoteCount;
    private Long downvoteCount;
    private Long commentCount;
    private Long viewCount;
    private Long shareCount;
    private Double popularity;
}
