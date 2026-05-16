package com.socialpulse.app.comment.infrastructure.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.socialpulse.app.comment.adapter.persistence.CommentRepositoryAdapter;
import com.socialpulse.app.comment.adapter.persistence.CommentReactionRepositoryAdapter;
import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.usecase.CreateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.DeleteCommentUseCase;
import com.socialpulse.app.comment.application.usecase.GetCommentRepliesUseCase;
import com.socialpulse.app.comment.application.usecase.ReactCommentUseCase;
import com.socialpulse.app.comment.application.usecase.UpdateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.domain.repository.CommentReactionRepository;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.application.service.CommentResponseAssembler;
import com.socialpulse.app.comment.application.service.CreateCommentService;
import com.socialpulse.app.comment.application.service.DeleteCommentService;
import com.socialpulse.app.comment.application.service.GetCommentRepliesService;
import com.socialpulse.app.comment.application.service.ReactCommentService;
import com.socialpulse.app.comment.application.service.UpdateCommentService;
import com.socialpulse.app.comment.application.service.ValidateParentCommentService;
import com.socialpulse.app.comment.infrastructure.persistence.mapper.CommentPersistenceMapper;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentReactionRepository;
import com.socialpulse.app.comment.infrastructure.persistence.repository.JpaCommentRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
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

	@Bean
	public CommentReactionRepository commentReactionRepository(
			JpaCommentReactionRepository jpaCommentReactionRepository,
			CommentPersistenceMapper commentPersistenceMapper) {
		return new CommentReactionRepositoryAdapter(jpaCommentReactionRepository, commentPersistenceMapper);
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
												 CommentResponseAssembler commentResponseAssembler,
												 CommentMapper commentMapper,
												 NotificationCommandService notificationCommandService) {
		return new CreateCommentService(commentRepositoryPort, postRepositoryPort, userRepositoryPort,
				validateParentCommentUseCase, commentResponseAssembler, commentMapper, notificationCommandService);
	}

	@Bean
	public com.socialpulse.app.comment.application.usecase.GetTopLevelCommentsUseCase getTopLevelCommentsUseCase(
			CommentRepository commentRepositoryPort,
			CommentResponseAssembler commentResponseAssembler) {
		return new com.socialpulse.app.comment.application.service.GetTopLevelCommentsService(
				commentRepositoryPort, commentResponseAssembler);
	}

	@Bean
	public GetCommentRepliesUseCase getCommentRepliesUseCase(
			CommentRepository commentRepositoryPort,
			CommentResponseAssembler commentResponseAssembler) {
		return new GetCommentRepliesService(commentRepositoryPort, commentResponseAssembler);
	}

	@Bean
	public ReactCommentUseCase reactCommentUseCase(
			CommentRepository commentRepositoryPort,
			CommentReactionRepository commentReactionRepository,
			UserRepository userRepositoryPort,
			CommentMapper commentMapper,
			NotificationCommandService notificationCommandService) {
		return new ReactCommentService(
				commentRepositoryPort,
				commentReactionRepository,
				userRepositoryPort,
				commentMapper,
				notificationCommandService);
	}

	@Bean
	public UpdateCommentUseCase updateCommentUseCase(CommentRepository commentRepositoryPort,
													 UserRepository userRepositoryPort,
													 CommentResponseAssembler commentResponseAssembler) {
		return new UpdateCommentService(commentRepositoryPort, userRepositoryPort, commentResponseAssembler);
	}

	@Bean
	public DeleteCommentUseCase deleteCommentUseCase(CommentRepository commentRepositoryPort,
													 PostRepository postRepositoryPort) {
		return new DeleteCommentService(commentRepositoryPort, postRepositoryPort);
	}

}
