package com.socialpulse.app.post.domain.model;

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
public class PostReactions {
	private Long id;
	private Long userId;
	private Long postId;
	private ReactionType reactionType;
	private LocalDateTime createdAt;

	public void changeReactionType(ReactionType reactionType) {
		this.reactionType = reactionType;
	}
}
