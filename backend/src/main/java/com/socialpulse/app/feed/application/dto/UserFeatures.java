package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserFeatures {
    private Long userId;
    private Double engagementRate;
    private Long postCount;
    private Long accountAgeDays;
}
