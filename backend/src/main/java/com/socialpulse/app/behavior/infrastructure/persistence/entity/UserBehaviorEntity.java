package com.socialpulse.app.behavior.infrastructure.persistence.entity;

import com.socialpulse.app.behavior.domain.enums.EventType;
import io.hypersistence.utils.hibernate.type.json.JsonBinaryType;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.annotations.Type;

import java.time.LocalDateTime;
import java.util.Map;

@Entity
@Table(name = "user_behaviors", indexes = {
        @Index(name = "idx_user_behaviors_user_time", columnList = "user_id, event_time"),
        @Index(name = "idx_user_behaviors_post_time", columnList = "post_id, event_time"),
        @Index(name = "idx_user_behaviors_event_type", columnList = "event_type"),
        @Index(name = "idx_user_behaviors_user_post", columnList = "user_id, post_id"),
        @Index(name = "idx_user_behaviors_time", columnList = "event_time"),
        @Index(name = "idx_user_behaviors_user_event_time", columnList = "user_id, event_type, event_time")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserBehaviorEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Enumerated(EnumType.STRING)
    @Column(name = "event_type", nullable = false, length = 50)
    private EventType eventType;

    @Column(name = "event_time", nullable = false)
    private LocalDateTime eventTime;

    @Column(name = "position")
    private Integer position;

    @Column(name = "dwell_time_seconds")
    private Integer dwellTimeSeconds;

    @Type(JsonBinaryType.class)
    @Column(name = "metadata", columnDefinition = "jsonb")
    private Map<String, Object> metadata;

    @PrePersist
    public void prePersist() {
        if (this.eventTime == null) {
            this.eventTime = LocalDateTime.now();
        }
    }
}
