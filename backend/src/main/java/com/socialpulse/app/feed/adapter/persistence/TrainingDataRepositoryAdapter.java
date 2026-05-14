package com.socialpulse.app.feed.adapter.persistence;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.stereotype.Component;

import com.socialpulse.app.feed.domain.model.TrainingDataRecord;
import com.socialpulse.app.feed.domain.repository.TrainingDataRepository;
import com.socialpulse.app.feed.infrastructure.persistence.repository.JpaTrainingDataRepository;

import lombok.RequiredArgsConstructor;

@Component
@RequiredArgsConstructor
public class TrainingDataRepositoryAdapter implements TrainingDataRepository {

    private final JpaTrainingDataRepository jpaRepository;

    @Override
    public TrainingDataRecord save(TrainingDataRecord record) {
        return jpaRepository.save(record);
    }

    @Override
    public List<TrainingDataRecord> findByImpressionTimeBetween(LocalDateTime startDate, LocalDateTime endDate) {
        return jpaRepository.findByImpressionTimeBetween(startDate, endDate);
    }

    @Override
    public long count() {
        return jpaRepository.count();
    }

    @Override
    public long countByRelevance(int relevance) {
        return jpaRepository.countByRelevance(relevance);
    }

    @Override
    public long countDistinctUsers() {
        return jpaRepository.countDistinctUsers();
    }

    @Override
    public long countDistinctPosts() {
        return jpaRepository.countDistinctPosts();
    }
}
