package com.socialpulse.app.topic.infrastructure.persistence.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.socialpulse.app.topic.infrastructure.persistence.entity.TopicFollowEntity;

public interface JpaTopicFollowRepository extends JpaRepository<TopicFollowEntity, Long> {

    List<TopicFollowEntity> findByUserId(Long userId);

    Optional<TopicFollowEntity> findByUserIdAndTopicSlug(Long userId, String topicSlug);

    boolean existsByUserIdAndTopicSlug(Long userId, String topicSlug);

    void deleteByUserIdAndTopicSlug(Long userId, String topicSlug);

    @Query("SELECT tf.topicSlug FROM TopicFollowEntity tf WHERE tf.userId = :userId")
    List<String> findFollowedTopicSlugsByUserId(@Param("userId") Long userId);
}
