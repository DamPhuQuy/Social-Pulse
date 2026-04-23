package com.socialpulse.app.feed.application.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRankedPost {
    private Long postId;
    private Double rankingScore;
    private Integer rank;
}
