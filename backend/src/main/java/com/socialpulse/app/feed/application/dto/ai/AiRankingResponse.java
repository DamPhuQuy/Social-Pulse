package com.socialpulse.app.feed.application.dto.ai;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiRankingResponse {
    private Long userId;
    private List<AiRankedPost> rankedPosts;
    private Integer totalCandidates;
    private String modelVersion;
    private String timestamp;
}
