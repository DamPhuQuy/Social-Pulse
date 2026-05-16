package com.socialpulse.app.feed.application.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AuthorFeatures {
    private Long authorId;
    private Double seniorityYears;
    private Long postCount;
    private Double averagePopularity;
}
