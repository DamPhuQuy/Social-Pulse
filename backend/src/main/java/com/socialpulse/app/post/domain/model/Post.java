package com.socialpulse.app.post.domain.model;

import java.time.LocalDateTime;

import com.socialpulse.app.post.domain.enums.Privacy;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Post {
	private Long id;
	private Long userId;
	private String content;
	private String imageUrl;
	private String imagePublicId;
	private Privacy privacy;
	private Long upvoteCount;
	private Long downvoteCount;
	private Long cmtCount;
	private Long viewCount;
	private Long shareCount;
	private Double hotScore;
	private boolean toxic;
	private Double toxicScore;
	private LocalDateTime createdAt;
	private LocalDateTime updatedAt;
	private LocalDateTime deletedAt;

	public void changePrivacy(Privacy newPrivacy) {
		this.privacy = newPrivacy;
	}

	public void incrementUpvoteCount() {
		this.upvoteCount = safeCount(this.upvoteCount) + 1L;
		updateHotScore();
	}

	public void decrementUpvoteCount() {
		this.upvoteCount = Math.max(0L, safeCount(this.upvoteCount) - 1L);
		updateHotScore();
	}

	public void incrementDownvoteCount() {
		this.downvoteCount = safeCount(this.downvoteCount) + 1L;
		updateHotScore();
	}

	public void decrementDownvoteCount() {
		this.downvoteCount = Math.max(0L, safeCount(this.downvoteCount) - 1L);
		updateHotScore();
	}

	private void updateHotScore() {
		this.hotScore = (double) (safeCount(this.upvoteCount) - safeCount(this.downvoteCount));
	}

	private long safeCount(Long value) {
		return value == null ? 0L : value;
	}
}


