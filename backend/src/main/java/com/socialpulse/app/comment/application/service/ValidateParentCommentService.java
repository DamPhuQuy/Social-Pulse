package com.socialpulse.app.comment.application.service;

import com.socialpulse.app.comment.application.port.in.ValidateParentCommentUseCase;
import com.socialpulse.app.comment.application.port.out.CommentRepositoryPort;
import com.socialpulse.app.comment.domain.model.Comment;
import com.socialpulse.app.common.exception.AppException;
import com.socialpulse.app.common.exception.status.CommentCode;

public class ValidateParentCommentService implements ValidateParentCommentUseCase {

    private final CommentRepositoryPort commentRepositoryPort;

    public ValidateParentCommentService(CommentRepositoryPort commentRepositoryPort) {
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
