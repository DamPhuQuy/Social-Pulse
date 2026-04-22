package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.comment.domain.model.Comment;

public interface ValidateParentCommentUseCase {
    Comment validateAndGetParentComment(Long postId, Long parentCommentId);
}

