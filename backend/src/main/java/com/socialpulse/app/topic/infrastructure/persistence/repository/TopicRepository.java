package com.socialpulse.app.topic.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicEntity;

@Repository
public interface TopicRepository extends JpaRepository<TopicEntity, Long> {
    Optional<TopicEntity> findBySlug(String slug);
    List<TopicEntity> findAllByOrderByNameAsc();
}
