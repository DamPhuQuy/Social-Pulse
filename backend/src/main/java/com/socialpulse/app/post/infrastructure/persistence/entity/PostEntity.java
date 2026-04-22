package com.socialpulse.app.post.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;

import com.socialpulse.app.post.domain.enums.Privacy;
import com.socialpulse.app.user.infrastructure.persistence.entity.UserEntity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@SQLDelete(sql = "UPDATE posts SET deleted_at = NOW() WHERE id = ?")
@Table(name = "posts", indexes = {
        @Index(name = "idx_hot_score", columnList = "hotScore"),
        @Index(name = "idx_post_user", columnList = "user_id"),
        @Index(name = "idx_post_created", columnList = "createdAt")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PostEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private UserEntity user;

    @Column(columnDefinition = "TEXT")
    private String content;

    private String imageUrl;
    private String imagePublicId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private Privacy privacy = Privacy.PUBLIC;

    @Builder.Default
    private Long upvoteCount = 0L;
    @Builder.Default
    private Long downvoteCount = 0L;
    @Builder.Default
    private Long cmtCount = 0L;
    @Builder.Default
    private Long viewCount = 0L;
    @Builder.Default
    private Long shareCount = 0L;
    @Builder.Default
    private Double hotScore = 0.0D;

    @Builder.Default
    private boolean toxic = false;
    @Builder.Default
    private Double toxicScore = 0.0D;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

    @Version
    private Long version;

    @PrePersist
    public void prePersist() {
        LocalDateTime now = LocalDateTime.now();
        this.createdAt = now;
        this.updatedAt = now;
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
