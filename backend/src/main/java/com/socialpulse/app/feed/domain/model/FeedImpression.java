package com.socialpulse.app.feed.domain.model;

import java.time.LocalDateTime;

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
@Table(name = "feed_impressions", indexes = {
    @Index(name = "idx_user_post", columnList = "user_id,post_id"),
    @Index(name = "idx_impression_time", columnList = "impression_time"),
    @Index(name = "idx_interacted", columnList = "interacted")
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class FeedImpression {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false)
    private Long userId;

    @Column(name = "post_id", nullable = false)
    private Long postId;

    @Column(name = "author_id", nullable = false)
    private Long authorId;

    @Column(name = "position_in_feed", nullable = false)
    private Integer positionInFeed;

    @Column(name = "ranking_strategy", length = 50)
    private String rankingStrategy;

    @Column(name = "impression_time", nullable = false)
    private LocalDateTime impressionTime;

    @Column(name = "interacted", nullable = false)
    private Boolean interacted;

    @Column(name = "interaction_time")
    private LocalDateTime interactionTime;

    @Column(name = "interaction_type", length = 50)
    private String interactionType;
}
