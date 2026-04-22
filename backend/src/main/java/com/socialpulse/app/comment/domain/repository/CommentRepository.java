package com.socialpulse.app.comment.domain.repository;

import java.util.Optional;

import com.socialpulse.app.comment.domain.model.Comment;

public interface CommentRepository {
	Optional<Comment> findById(Long id);

	Comment save(Comment comment);

}

