package com.socialpulse.app.share.infrastructure.persistence.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.user.domain.model.User;

import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "shares")
@Getter
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class ShareEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
