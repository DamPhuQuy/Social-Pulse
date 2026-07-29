package com.socialpulse.app.comment.application.service;
import org.springframework.stereotype.Service;

import com.socialpulse.app.realtime.application.service.SseEmitterRegistry;
import java.util.Map;

import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.dto.request.CommentCreationRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.usecase.CreateCommentUseCase;
import com.socialpulse.app.comment.application.usecase.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.PostCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.feed.domain.repository.UserInteractionRepository;
import com.socialpulse.app.notification.application.service.NotificationCommandService;
import org.springframework.transaction.annotation.Transactional;
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.post.domain.model.Post;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.model.User;

@Service
public class CreateCommentService implements CreateCommentUseCase {

    private final CommentRepository commentRepositoryPort;
    private final PostRepository postRepositoryPort;
    private final UserRepository userRepositoryPort;
    private final ValidateParentCommentUseCase validateParentCommentUseCase;
    private final CommentResponseAssembler commentResponseAssembler;
    private final CommentMapper commentMapper;
    private final NotificationCommandService notificationCommandService;
    private final SseEmitterRegistry sseEmitterRegistry;
    private final UserInteractionRepository userInteractionRepository;

    public CreateCommentService(CommentRepository commentRepositoryPort,
                                PostRepository postRepositoryPort,
                                UserRepository userRepositoryPort,
                                ValidateParentCommentUseCase validateParentCommentUseCase,
                                CommentResponseAssembler commentResponseAssembler,
                                CommentMapper commentMapper,
                                NotificationCommandService notificationCommandService,
                                SseEmitterRegistry sseEmitterRegistry,
                                UserInteractionRepository userInteractionRepository) {
        this.commentRepositoryPort = commentRepositoryPort;
        this.postRepositoryPort = postRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.validateParentCommentUseCase = validateParentCommentUseCase;
        this.commentResponseAssembler = commentResponseAssembler;
        this.commentMapper = commentMapper;
        this.notificationCommandService = notificationCommandService;
        this.sseEmitterRegistry = sseEmitterRegistry;
        this.userInteractionRepository = userInteractionRepository;
    }

    @Override
    @Transactional
    public CommentCreationResponse createComment(Long postId, CommentCreationRequest request, CustomUserDetails currentUser) {
        Post post = postRepositoryPort.findById(postId)
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        validatePostAccessible(post, currentUser);

        User user = userRepositoryPort.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Comment parent = validateParentCommentUseCase
                .validateAndGetParentComment(postId, request.getParentCommentId());

        Comment comment = commentMapper.toComment(postId, request, user.getId(), parent == null ? null : parent.getId());

        Comment savedComment = commentRepositoryPort.save(comment);
        post.incrementCommentCount();
        postRepositoryPort.save(post);

        sseEmitterRegistry.broadcast("post_stats", Map.of("postId", postId, "cmtCount", post.getCmtCount()));

        // Record interaction for personalized feed ranking
        if (!user.getId().equals(post.getUserId())) {
            userInteractionRepository.save(user.getId(), post.getUserId(), "COMMENT");
        }
        if (parent == null) {
            notificationCommandService.notifyCommentOnPost(user.getId(), post.getUserId(), postId, savedComment.getId());
        } else {
            notificationCommandService.notifyReply(user.getId(), parent.getUserId(), savedComment.getId());
        }

        return commentResponseAssembler.toCommentCreationResponse(savedComment, user);
    }

    private void validatePostAccessible(Post post, CustomUserDetails currentUser) {
        if (post.getDeletedAt() != null) {
            throw new AppException(PostCode.POST_NOT_FOUND);
        }

        boolean canAccess = post.isPublic()
                || post.getUserId().equals(currentUser.getId())
                || currentUser.getAuthorities().stream()
                        .anyMatch(authority -> authority.getAuthority().equals("post:manage"));

        if (!canAccess) {
            throw new AppException(PostCode.POST_NOT_ACCESSIBLE);
        }
    }
}
