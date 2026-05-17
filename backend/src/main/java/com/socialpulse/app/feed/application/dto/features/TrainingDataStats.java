package com.socialpulse.app.feed.application.dto.features;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataStats {
    private long totalSamples;
    private long positiveSamples;
    private long negativeSamples;
    private double positiveRate;
    private long uniqueUsers;
    private long uniquePosts;
}
