package com.socialpulse.app.comment.application.service;

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
import com.socialpulse.app.post.domain.repository.PostRepository;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.repository.UserRepository;
import com.socialpulse.app.user.domain.model.User;

public class CreateCommentService implements CreateCommentUseCase {

    private final CommentRepository commentRepositoryPort;
    private final PostRepository postRepositoryPort;
    private final UserRepository userRepositoryPort;
    private final ValidateParentCommentUseCase validateParentCommentUseCase;
    private final CommentMapper commentMapper;

    public CreateCommentService(CommentRepository commentRepositoryPort,
                                PostRepository postRepositoryPort,
                                UserRepository userRepositoryPort,
                                ValidateParentCommentUseCase validateParentCommentUseCase,
                                CommentMapper commentMapper) {
        this.commentRepositoryPort = commentRepositoryPort;
        this.postRepositoryPort = postRepositoryPort;
        this.userRepositoryPort = userRepositoryPort;
        this.validateParentCommentUseCase = validateParentCommentUseCase;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentCreationResponse createComment(CommentCreationRequest request, CustomUserDetails currentUser) {
        postRepositoryPort.findById(request.getPostId())
                .orElseThrow(() -> new AppException(PostCode.POST_NOT_FOUND));

        User user = userRepositoryPort.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        Comment parent = validateParentCommentUseCase
                .validateAndGetParentComment(request.getPostId(), request.getParentCommentId());

        Comment comment = commentMapper.toComment(request, user.getId(), parent == null ? null : parent.getId());

        Comment savedComment = commentRepositoryPort.save(comment);

        return commentMapper.toCommentCreationResponse(savedComment, user);
    }
}


