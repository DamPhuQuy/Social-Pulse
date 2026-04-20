package com.socialpulse.app.comment.domain.model;

import java.time.LocalDateTime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Comment {
	private Long id;
	private Long postId;
	private Long userId;
	private Long parentCommentId;
	private String content;
	private LocalDateTime createdAt;
	private Long upvoteCount;
	private Long downvoteCount;
	private boolean deleted;

	public void incrementUpvoteCount() {
		this.upvoteCount = safeCount(this.upvoteCount) + 1L;
	}

	public void decrementUpvoteCount() {
		this.upvoteCount = Math.max(0L, safeCount(this.upvoteCount) - 1L);
	}

	public void incrementDownvoteCount() {
		this.downvoteCount = safeCount(this.downvoteCount) + 1L;
	}

	public void decrementDownvoteCount() {
		this.downvoteCount = Math.max(0L, safeCount(this.downvoteCount) - 1L);
	}

	public void markDeleted() {
		this.deleted = true;
	}

	private long safeCount(Long value) {
		return value == null ? 0L : value;
	}
}
