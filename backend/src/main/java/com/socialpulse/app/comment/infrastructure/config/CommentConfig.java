package com.socialpulse.app.comment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.comment.adapter.out.CommentRepositoryAdapter;
import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.port.in.CreateCommentUseCase;
import com.socialpulse.app.comment.application.port.in.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.application.port.out.CommentRepositoryPort;
import com.socialpulse.app.comment.application.service.CreateCommentService;
import com.socialpulse.app.comment.application.service.ValidateParentCommentService;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentDomainToEntity;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentEntityToDomain;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;
import com.socialpulse.app.post.application.port.out.PostRepositoryPort;
import com.socialpulse.app.user.application.port.out.UserRepositoryPort;

@Configuration
public class CommentConfig {

    // adapters --------------------------------------

	@Bean
	public CommentRepositoryPort commentRepositoryPort(JpaCommentRepository jpaCommentRepository,
													   CommentEntityToDomain commentEntityToDomain,
													   CommentDomainToEntity commentDomainToEntity) {
		return new CommentRepositoryAdapter(jpaCommentRepository, commentEntityToDomain, commentDomainToEntity);
	}

    // use cases --------------------------------------

	@Bean
	public ValidateParentCommentUseCase validateParentCommentUseCase(CommentRepositoryPort commentRepositoryPort) {
		return new ValidateParentCommentService(commentRepositoryPort);
	}

	@Bean
	public CreateCommentUseCase createCommentUseCase(CommentRepositoryPort commentRepositoryPort,
													 PostRepositoryPort postRepositoryPort,
													 UserRepositoryPort userRepositoryPort,
												 ValidateParentCommentUseCase validateParentCommentUseCase,
												 CommentMapper commentMapper) {
		return new CreateCommentService(commentRepositoryPort, postRepositoryPort, userRepositoryPort,
				validateParentCommentUseCase, commentMapper);
	}

}
