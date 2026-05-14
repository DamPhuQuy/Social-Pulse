package com.socialpulse.app.comment.application.service;

import com.socialpulse.app.comment.application.dto.mapper.CommentMapper;
import com.socialpulse.app.comment.application.dto.request.CommentUpdateRequest;
import com.socialpulse.app.comment.application.dto.response.CommentCreationResponse;
import com.socialpulse.app.comment.application.usecase.UpdateCommentUseCase;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;
import com.socialpulse.app.common.exception.status.UserCode;
import com.socialpulse.app.security.user.CustomUserDetails;
import com.socialpulse.app.user.domain.model.User;
import com.socialpulse.app.user.domain.repository.UserRepository;

public class UpdateCommentService implements UpdateCommentUseCase {

    private final CommentRepository commentRepository;
    private final UserRepository userRepository;
    private final CommentMapper commentMapper;

    public UpdateCommentService(CommentRepository commentRepository,
                                UserRepository userRepository,
                                CommentMapper commentMapper) {
        this.commentRepository = commentRepository;
        this.userRepository = userRepository;
        this.commentMapper = commentMapper;
    }

    @Override
    public CommentCreationResponse updateComment(Long postId, Long commentId, CommentUpdateRequest request, CustomUserDetails currentUser) {
        Comment comment = commentRepository.findById(commentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        if (!comment.getPostId().equals(postId)) {
            throw new AppException(CommentCode.COMMENT_NOT_BELONG_TO_POST);
        }

        if (!comment.getUserId().equals(currentUser.getId())) {
            throw new AppException(CommentCode.COMMENT_NOT_OWNER);
        }

        if (comment.isDeleted()) {
            throw new AppException(CommentCode.CANNOT_EDIT_DELETED_COMMENT);
        }

        comment.updateContent(request.getContent());

        Comment updatedComment = commentRepository.save(comment);

        User user = userRepository.findById(currentUser.getId())
                .orElseThrow(() -> new AppException(UserCode.USER_NOT_FOUND));

        return commentMapper.toCommentCreationResponse(updatedComment, user);
    }
}
