package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

import com.socialpulse.app.feed.domain.enums.RankingProvider;
import com.socialpulse.app.feed.domain.enums.Source;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedItem {
    private Long postId;
    private Long userId;
    private Double aiScore;
    private Source source;
    private RankingProvider rankingProvider;
    private String featureSchemaVersion;
    private LocalDateTime rankedAt;
    private Double affinityScore;
    private Long interactionCount30d;
}
