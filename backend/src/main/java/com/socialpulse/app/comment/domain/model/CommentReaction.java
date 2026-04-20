package com.socialpulse.app.comment.domain.model;

import java.time.LocalDateTime;

import com.socialpulse.app.common.utils.ReactionType;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CommentReaction {
	private Long id;
	private Long commentId;
	private Long userId;
	private ReactionType reactionType;
	private LocalDateTime createdAt;

	public void changeReactionType(ReactionType reactionType) {
		this.reactionType = reactionType;
	}

}
