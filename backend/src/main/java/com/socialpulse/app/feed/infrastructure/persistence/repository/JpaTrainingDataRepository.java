package com.socialpulse.app.feed.infrastructure.persistence.repository;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.feed.domain.model.TrainingDataRecord;

public interface JpaTrainingDataRepository extends JpaRepository<TrainingDataRecord, Long> {

    List<TrainingDataRecord> findByImpressionTimeBetween(LocalDateTime startDate, LocalDateTime endDate);

    long countByRelevance(int relevance);

    @Query("SELECT COUNT(DISTINCT td.userId) FROM TrainingDataRecord td")
    long countDistinctUsers();

    @Query("SELECT COUNT(DISTINCT td.postId) FROM TrainingDataRecord td")
    long countDistinctPosts();
}
