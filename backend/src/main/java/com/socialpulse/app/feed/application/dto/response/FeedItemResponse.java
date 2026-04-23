package com.socialpulse.app.feed.application.dto.response;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedItemResponse {
    private Long postId;
    private Double aiScore;
    private String source;
    private LocalDateTime rankedAt;
}
