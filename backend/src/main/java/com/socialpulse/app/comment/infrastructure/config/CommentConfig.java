package com.socialpulse.app.comment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.comment.adapter.persistence.CommentRepositoryAdapter;
import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.usecase.CreateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.application.service.CreateCommentService;
import com.socialpulse.app.comment.application.service.ValidateParentCommentService;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.user.domain.repository.UserRepository;

@Configuration
public class CommentConfig {

    // adapters --------------------------------------

	@Bean
	public CommentRepository commentRepositoryPort(JpaCommentRepository jpaCommentRepository,
									CommentPersistenceMapper commentPersistenceMapper) {
		return new CommentRepositoryAdapter(jpaCommentRepository, commentPersistenceMapper);
	}

    // use cases --------------------------------------

	@Bean
	public ValidateParentCommentUseCase validateParentCommentUseCase(CommentRepository commentRepositoryPort) {
		return new ValidateParentCommentService(commentRepositoryPort);
	}

	@Bean
	public CreateCommentUseCase createCommentUseCase(CommentRepository commentRepositoryPort,
													 PostRepository postRepositoryPort,
													 UserRepository userRepositoryPort,
												 ValidateParentCommentUseCase validateParentCommentUseCase,
												 CommentMapper commentMapper) {
		return new CreateCommentService(commentRepositoryPort, postRepositoryPort, userRepositoryPort,
				validateParentCommentUseCase, commentMapper);
	}

}


