package com.socialpulse.app.comment.adapter.persistence;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.data.domain.PageRequest;

import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;

@Repository
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

	@Override
	public List<Comment> findTopLevelCommentsByPostId(Long postId, long lastId, int limit) {
		return jpaCommentRepository.findTopLevelCommentsByPostId(postId, lastId, PageRequest.of(0, limit))
				.stream()
				.map(commentPersistenceMapper::toDomain)
				.collect(Collectors.toList());
	}

	@Override
	public List<Comment> findRepliesByParentCommentId(Long postId, Long parentCommentId, long lastId, int limit) {
		return jpaCommentRepository.findRepliesByParentCommentId(postId, parentCommentId, lastId, PageRequest.of(0, limit))
				.stream()
				.map(commentPersistenceMapper::toDomain)
				.collect(Collectors.toList());
	}

	@Override
	public Map<Long, Long> countRepliesByParentCommentIds(Set<Long> parentCommentIds) {
		if (parentCommentIds == null || parentCommentIds.isEmpty()) {
			return Map.of();
		}
		return jpaCommentRepository.countRepliesByParentCommentIds(parentCommentIds).stream()
				.collect(Collectors.toMap(
						row -> (Long) row[0],
						row -> (Long) row[1]
				));
	}

}
