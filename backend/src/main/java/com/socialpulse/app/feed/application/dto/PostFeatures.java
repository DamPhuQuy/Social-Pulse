package com.socialpulse.app.feed.application.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostFeatures {
    private Long postId;
    private Long upvoteCount;
    private Long downvoteCount;
    private Long cmtCount;
    private Long viewCount;
    private Long shareCount;
    private Double hotScore;
    private Double recencyScore;
    private String postType;
}
