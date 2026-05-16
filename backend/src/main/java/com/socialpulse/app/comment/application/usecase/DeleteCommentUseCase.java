package com.socialpulse.app.comment.application.usecase;

import com.socialpulse.app.security.user.CustomUserDetails;

public interface DeleteCommentUseCase {
    void deleteComment(Long postId, Long commentId, CustomUserDetails currentUser);
}
