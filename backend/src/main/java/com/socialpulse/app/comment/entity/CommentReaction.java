package com.socialpulse.app.comment.entity;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;

@Entity
@Table(name = "comment_reactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"comment_id", "user_id"})
}, indexes = {
        @Index(name = "idx_comment_reaction_comment", columnList = "comment_id"),
        @Index(name = "idx_comment_reaction_user", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class CommentReaction {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "comment_id", nullable = false)
    private Comment comment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ReactionType reactionType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
