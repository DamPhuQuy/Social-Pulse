package com.socialpulse.app.reaction.entity;

import com.socialpulse.app.comment.entity.Comment;
import com.socialpulse.app.user.entity.User;

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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(name = "reactioncmt", indexes = {
    @Index(name = "idx_reactioncmt_cmt_id", columnList = "cmt_id"),
    @Index(name = "idx_reactioncmt_user_id", columnList = "user_id"),
    @Index(name = "idx_reactioncmt_type", columnList = "reactiontype")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ReactionComment {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cmt_id", nullable = false)
    private Comment comment;

    @NotNull
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "reactiontype")
    private Boolean reactionType; // true: upvote, false: downvote
}
