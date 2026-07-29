package com.socialpulse.app.comment.application.service;
import org.springframework.stereotype.Service;

import com.socialpulse.app.comment.application.usecase.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.domain.repository.CommentRepository;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;

@Service
public class ValidateParentCommentService implements ValidateParentCommentUseCase {

    private final CommentRepository commentRepositoryPort;

    public ValidateParentCommentService(CommentRepository commentRepositoryPort) {
        this.commentRepositoryPort = commentRepositoryPort;
    }

    @Override
    public Comment validateAndGetParentComment(Long postId, Long parentCommentId) {
        if (parentCommentId == null) {
            return null;
        }

        Comment parent = commentRepositoryPort.findById(parentCommentId)
                .orElseThrow(() -> new AppException(CommentCode.COMMENT_NOT_FOUND));

        if (parent.getParentCommentId() != null) {
            throw new AppException(CommentCode.REPLY_TO_COMMENT_NOT_ALLOWED);
        }

        if (!postId.equals(parent.getPostId())) {
            throw new AppException(CommentCode.PARENT_MUST_BELONG_TO_SAME_POST);
        }

        if (parent.isDeleted()) {
            throw new AppException(CommentCode.CANNOT_REPLY_TO_DELETED_COMMENT);
        }

        return parent;
    }
}


