package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import com.socialpulse.app.feed.domain.model.FeatureSnapshot;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "training_data", indexes = {
    @Index(name = "idx_user_id", columnList = "user_id"),
    @Index(name = "idx_post_id", columnList = "post_id"),
    @Index(name = "idx_impression_time", columnList = "impression_time"),
    @Index(name = "idx_relevance", columnList = "relevance")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrainingDataRecord {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "features", columnDefinition = "jsonb", nullable = false)
    private FeatureSnapshot features;

    @Column(name = "relevance", nullable = false)
    private Integer relevance;

    @Column(name = "impression_time", nullable = false)
    private LocalDateTime impressionTime;

    @Column(name = "interaction_time")
    private LocalDateTime interactionTime;

    @Column(name = "interaction_type", length = 50)
    private String interactionType;

    @Column(name = "position_in_feed")
    private Integer positionInFeed;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
