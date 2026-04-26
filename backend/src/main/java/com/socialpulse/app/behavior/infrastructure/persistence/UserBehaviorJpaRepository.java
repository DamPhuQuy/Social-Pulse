package com.socialpulse.app.behavior.infrastructure.persistence;

import com.socialpulse.app.behavior.domain.enums.EventType;
import com.socialpulse.app.behavior.infrastructure.persistence.entity.UserBehaviorEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface UserBehaviorJpaRepository extends JpaRepository<UserBehaviorEntity, Long> {

    @Query("SELECT ub FROM UserBehaviorEntity ub WHERE ub.userId = :userId AND ub.eventTime >= :since ORDER BY ub.eventTime DESC")
    List<UserBehaviorEntity> findByUserIdAndEventTimeSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);

    @Query("SELECT ub FROM UserBehaviorEntity ub WHERE ub.postId = :postId AND ub.eventTime >= :since ORDER BY ub.eventTime DESC")
    List<UserBehaviorEntity> findByPostIdAndEventTimeSince(@Param("postId") Long postId, @Param("since") LocalDateTime since);

    @Query("SELECT ub FROM UserBehaviorEntity ub WHERE ub.userId = :userId AND ub.postId IN :postIds AND ub.eventTime >= :since")
    List<UserBehaviorEntity> findByUserIdAndPostIdsAndEventTimeSince(
            @Param("userId") Long userId,
            @Param("postIds") List<Long> postIds,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT COUNT(ub) FROM UserBehaviorEntity ub WHERE ub.userId = :userId AND ub.postId = :postId AND ub.eventType IN :eventTypes AND ub.eventTime >= :since")
    Long countInteractions(
            @Param("userId") Long userId,
            @Param("postId") Long postId,
            @Param("eventTypes") List<EventType> eventTypes,
            @Param("since") LocalDateTime since
    );

    @Query("SELECT ub.postId, COUNT(ub) FROM UserBehaviorEntity ub WHERE ub.userId = :userId AND ub.eventType IN :eventTypes AND ub.eventTime >= :since GROUP BY ub.postId")
    List<Object[]> countInteractionsByPost(
            @Param("userId") Long userId,
            @Param("eventTypes") List<EventType> eventTypes,
            @Param("since") LocalDateTime since
    );
}
