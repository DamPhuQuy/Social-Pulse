package com.socialpulse.app.comment.adapter.out;

import java.util.Optional;

import com.socialpulse.app.comment.application.port.out.CommentRepositoryPort;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentDomainToEntity;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentEntityToDomain;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;

public class CommentRepositoryAdapter implements CommentRepositoryPort {

	private final JpaCommentRepository jpaCommentRepository;
	private final CommentEntityToDomain commentEntityToDomain;
	private final CommentDomainToEntity commentDomainToEntity;

	public CommentRepositoryAdapter(JpaCommentRepository jpaCommentRepository,
									CommentEntityToDomain commentEntityToDomain,
									CommentDomainToEntity commentDomainToEntity) {
		this.jpaCommentRepository = jpaCommentRepository;
		this.commentEntityToDomain = commentEntityToDomain;
		this.commentDomainToEntity = commentDomainToEntity;
	}

	@Override
	public Optional<Comment> findById(Long id) {
		return jpaCommentRepository.findById(id)
				.map(commentEntityToDomain::toDomain);
	}

	@Override
	public Comment save(Comment comment) {
		return commentEntityToDomain.toDomain(
				jpaCommentRepository.save(commentDomainToEntity.toEntity(comment))
		);
	}

}
