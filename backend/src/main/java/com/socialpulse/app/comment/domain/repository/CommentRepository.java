package com.socialpulse.app.comment.domain.repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.socialpulse.app.comment.domain.model.Comment;

public interface CommentRepository {
	Optional<Comment> findById(Long id);

	Comment save(Comment comment);

	List<Comment> findTopLevelCommentsByPostId(Long postId, long lastId, int limit);

	List<Comment> findRepliesByParentCommentId(Long postId, Long parentCommentId, long lastId, int limit);

	Map<Long, Long> countRepliesByParentCommentIds(Set<Long> parentCommentIds);
}
