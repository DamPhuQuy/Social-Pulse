package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RankingFeatures {
    private Long postId;
    private PostFeatures postFeatures;
    private UserFeatures authorFeatures;
    private UserFeatures viewerFeatures;
}
