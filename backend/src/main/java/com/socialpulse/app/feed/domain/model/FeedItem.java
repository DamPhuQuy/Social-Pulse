package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

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
    private String source; // RECENT, FOLLOWING, POPULAR, RANDOM
    private LocalDateTime rankedAt;
}
