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
    private String topic;
    private String createdAt;
    private Integer contentLength;
    private Boolean hasImage;
    private Boolean hasVideo;
    private Integer authorFollowerCount;
    private Double authorAvgEngagementRate;
    private Double predictedQualityScore;
}
