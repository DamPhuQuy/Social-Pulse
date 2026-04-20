package com.socialpulse.app.comment.application.port.out;

import java.util.Optional;

import com.socialpulse.app.comment.domain.model.Comment;

public interface CommentRepositoryPort {
	Optional<Comment> findById(Long id);

	Comment save(Comment comment);

}
