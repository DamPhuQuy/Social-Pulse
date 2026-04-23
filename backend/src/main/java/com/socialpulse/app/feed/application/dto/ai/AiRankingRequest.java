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
public class AiRankingRequest {
    private Long userId;
    private AiUserFeatures userFeatures;
    private List<AiPostFeatures> candidatePosts;
    private List<AiRelationshipFeatures> relationships;
}
