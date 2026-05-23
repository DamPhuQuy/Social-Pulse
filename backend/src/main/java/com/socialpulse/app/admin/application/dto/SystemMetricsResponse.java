package com.socialpulse.app.admin.application.dto;

import java.time.LocalDateTime;
import java.util.Map;

import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SystemMetricsResponse {
    private LocalDateTime generatedAt;
    private String period;       // e.g. "LAST_7_DAYS", "LAST_30_DAYS", "ALL_TIME"

    // User analytics
    private long totalUsers;
    private long newUsers;
    private Map<String, Long> usersByStatus;

    // Content analytics
    private long totalPosts;
    private long newPosts;
    private long toxicPosts;
    private long deletedPosts;

    // Feed analytics
    private long totalFeedImpressions;
    private long newFeedImpressions;
    private long aiRankedImpressions;
    private long fallbackRankedImpressions;
}
