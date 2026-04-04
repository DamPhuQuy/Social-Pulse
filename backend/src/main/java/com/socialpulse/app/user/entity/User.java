package com.socialpulse.app.user.entity;

import java.time.LocalDateTime;
import java.util.List;

import com.socialpulse.app.comment.entity.Comment;
import com.socialpulse.app.comment.entity.CommentReaction;
import com.socialpulse.app.post.entity.Post;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String username;

    @OneToOne(mappedBy = "user", cascade = CascadeType.ALL)
    private UserProfile profile;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Post> posts;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<Comment> comments;

    @OneToMany(mappedBy = "user", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private List<CommentReaction> commentReactions;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(nullable = false)
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserStatus status;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private UserRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VerificationStatus verification;

    private boolean isLocked; 

    @Column(name = "failed_attempts", nullable = false)
    private int failedLoginAttempts;

    private LocalDateTime lastLoginAt;

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

    public void incrementFailedLoginAttempts() {
        this.failedLoginAttempts++;
    }

    public void resetFailedLoginAttempts() {
        this.failedLoginAttempts = 0;
    }

    // Ghi lại thời điểm đăng nhập thành công gần nhất
    public void updateLastLoginAt() {
        this.lastLoginAt = LocalDateTime.now();
    }

    public void pendingAccount() {
        this.status = UserStatus.PENDING;
    }

    public void activeAccount() {
        this.status = UserStatus.ACTIVE;
    }

    public void verifyAccount() { this.verification = VerificationStatus.VERIFIED; }

    public void lockAccount() {
        this.status = UserStatus.LOCKED;
    }

    @PrePersist
    public void prePersist() {
        if (this.role == null) {
            this.role = UserRole.USER;
        }

        if (this.verification == null) {
            this.verification = VerificationStatus.NOT_VERIFIED;
        }

        if (this.status == null) {
            this.status = UserStatus.PENDING;
        }

        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    public void preUpdate() {
        this.updatedAt = LocalDateTime.now();
    }
}
