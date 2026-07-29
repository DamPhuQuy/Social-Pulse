package com.socialpulse.app.feed.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "feed_impressions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FeedImpressionEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "viewer_id", nullable = false)
    private Long viewerId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "rank_position", nullable = false)
    private Integer rankPosition;

    @Column(name = "page_number", nullable = false)
    private Integer pageNumber;

    @Column(name = "page_size", nullable = false)
    private Integer pageSize;

    @Column(name = "ranking_score")
    private Double rankingScore;

    @Column(name = "candidate_source", length = 20)
    private String candidateSource;

    @Column(name = "ranking_provider", nullable = false, length = 20)
    private String rankingProvider;

    @Column(name = "feature_schema_version", nullable = false, length = 16)
    private String featureSchemaVersion;

    @Column(name = "feed_context", nullable = false, length = 64)
    private String feedContext;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
