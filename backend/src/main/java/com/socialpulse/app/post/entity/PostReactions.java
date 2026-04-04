package com.socialpulse.app.post.entity;

import com.socialpulse.app.common.utils.ReactionType;
import com.socialpulse.app.user.entity.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;
import java.time.LocalDateTime;

@Entity
@Table(name = "post_reactions", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"post_id", "user_id"})
}, indexes = {
        @Index(name = "idx_post_reaction_post", columnList = "post_id"),
        @Index(name = "idx_post_reaction_user", columnList = "user_id")
})
@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@Builder
public class PostReactions {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "post_id", nullable = false)
    private Post post;

    @Enumerated(EnumType.STRING)
    private ReactionType reactionType;

    @CreationTimestamp
    private LocalDateTime createdAt;
}
