package com.socialpulse.app.comment.entity;

import java.time.LocalDateTime;

import org.hibernate.annotations.CreationTimestamp;

import com.socialpulse.app.post.entity.Post;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "comment", indexes = {
    @Index(name = "idx_comment_postid", columnList = "postid"),
    @Index(name = "idx_comment_createat", columnList = "createat")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "postid", nullable = false)
    private Post post;

    @Size(max = 5000)
    @Column(length = 5000)
    private String message;

    @CreationTimestamp
    @Column(name = "createat", nullable = false, updatable = false)
    private LocalDateTime createAt;
}
