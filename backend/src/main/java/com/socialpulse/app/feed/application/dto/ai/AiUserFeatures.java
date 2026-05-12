package com.socialpulse.app.feed.application.dto.ai;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiUserFeatures {
    private Long userId;
    private Integer postCount;
    private Integer accountAgeDays;
    private Double engagementRate;
    private Double avgSessionDurationMinutes;
}
