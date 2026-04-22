package com.socialpulse.app.comment.adapter.persistence;

import java.util.Optional;

import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;

public class CommentRepositoryAdapter implements CommentRepository {

	private final JpaCommentRepository jpaCommentRepository;
	private final CommentPersistenceMapper commentPersistenceMapper;

	public CommentRepositoryAdapter(JpaCommentRepository jpaCommentRepository,
									CommentPersistenceMapper commentPersistenceMapper) {
		this.jpaCommentRepository = jpaCommentRepository;
		this.commentPersistenceMapper = commentPersistenceMapper;
	}

	@Override
	public Optional<Comment> findById(Long id) {
		return jpaCommentRepository.findById(id)
				.map(commentPersistenceMapper::toDomain);
	}

	@Override
	public Comment save(Comment comment) {
		return commentPersistenceMapper.toDomain(
				jpaCommentRepository.save(commentPersistenceMapper.toEntity(comment))
		);
	}

}


