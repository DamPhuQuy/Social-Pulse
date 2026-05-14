package com.socialpulse.app.feed.domain.repository;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.feed.domain.model.TrainingDataRecord;

public interface TrainingDataRepository {
    TrainingDataRecord save(TrainingDataRecord record);
    List<TrainingDataRecord> findByImpressionTimeBetween(LocalDateTime startDate, LocalDateTime endDate);
    long count();
    long countByRelevance(int relevance);
    long countDistinctUsers();
    long countDistinctPosts();
}
