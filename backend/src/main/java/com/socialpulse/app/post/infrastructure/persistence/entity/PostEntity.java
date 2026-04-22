package com.socialpulse.app.post.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.SQLDelete;

import com.socialpulse.app.post.domain.enums.PostType;
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

    @Column(name = "parent_post_id")
    private Long parentPostId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private PostType type = PostType.ORIGINAL;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Privacy privacy = Privacy.PUBLIC;

    private Long upvoteCount = 0L;
    private Long downvoteCount = 0L;
    private Long cmtCount = 0L;
    private Long viewCount = 0L;
    private Long shareCount = 0L;
    private Double hotScore = 0.0D;

    private boolean toxic = false;
    private Double toxicScore = 0.0D;

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private LocalDateTime deletedAt;

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
